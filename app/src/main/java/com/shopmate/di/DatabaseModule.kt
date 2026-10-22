package com.shopmate.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shopmate.data.local.AppDatabase
import com.shopmate.data.local.dao.CategoryDao
import com.shopmate.data.local.dao.ProductBarcodeDao
import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.local.dao.ReceiptCounterDao
import com.shopmate.data.local.dao.ReportDao
import com.shopmate.data.local.dao.SaleDao
import com.shopmate.data.local.dao.StockCountDao
import com.shopmate.data.local.dao.StockDeltaDao
import com.shopmate.data.local.dao.StockInsightDao
import com.shopmate.data.local.dao.SyncEventDao
import com.shopmate.data.local.dao.UnknownBarcodeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.UUID
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** v1 → v2: adds the daily receipt counter table (Phase 3). Preserves existing data. */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `receipt_counter` (" +
                    "`date` TEXT NOT NULL, " +
                    "`last_number` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`date`))"
            )
        }
    }

    /** v2 → v3: adds the stock movement ledger (Phase 4). Preserves existing data. */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `stock_deltas` (" +
                    "`id` TEXT NOT NULL, " +
                    "`product_id` TEXT NOT NULL, " +
                    "`delta` INTEGER NOT NULL, " +
                    "`reason` TEXT NOT NULL, " +
                    "`note` TEXT, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "`synced` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_stock_deltas_product_id` " +
                    "ON `stock_deltas` (`product_id`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_stock_deltas_created_at` " +
                    "ON `stock_deltas` (`created_at`)"
            )
        }
    }

    /**
     * v3 → v4: the schema debt CLAUDE.md § Schema debt names — pricing history and supplier on
     * `products`, one product / many barcodes, unknown codes rung up at the till, and stocktake
     * sessions. Products and Stock cannot be honest without these. Preserves existing data.
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // pricing history + supplier
            db.execSQL("ALTER TABLE `products` ADD COLUMN `previous_cost_price` TEXT")
            db.execSQL("ALTER TABLE `products` ADD COLUMN `cost_price_changed_at` INTEGER")
            db.execSQL("ALTER TABLE `products` ADD COLUMN `selling_price_set_at` INTEGER")
            db.execSQL("ALTER TABLE `products` ADD COLUMN `supplier` TEXT")

            // one product, many barcodes
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `product_barcodes` (" +
                    "`id` TEXT NOT NULL, " +
                    "`product_id` TEXT NOT NULL, " +
                    "`code` TEXT NOT NULL, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_product_barcodes_code` " +
                    "ON `product_barcodes` (`code`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_product_barcodes_product_id` " +
                    "ON `product_barcodes` (`product_id`)"
            )
            // Backfill existing single barcodes. `products.barcode` stays populated for
            // compatibility; lookups go through product_barcodes from here on.
            db.execSQL(
                "INSERT OR IGNORE INTO `product_barcodes` (`id`, `product_id`, `code`, `created_at`) " +
                    "SELECT hex(randomblob(16)), `id`, `barcode`, `updated_at` " +
                    "FROM `products` WHERE `barcode` IS NOT NULL AND `barcode` != ''"
            )

            // unknown codes rung up at the till
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `unknown_barcodes` (" +
                    "`code` TEXT NOT NULL, " +
                    "`hits` INTEGER NOT NULL, " +
                    "`first_seen_at` INTEGER NOT NULL, " +
                    "`last_seen_at` INTEGER NOT NULL, " +
                    "`last_till` TEXT, " +
                    "PRIMARY KEY(`code`))"
            )

            // stocktake sessions
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `stock_counts` (" +
                    "`id` TEXT NOT NULL, " +
                    "`started_at` INTEGER NOT NULL, " +
                    "`committed_at` INTEGER, " +
                    "`counted_by` TEXT, " +
                    "`item_count` INTEGER NOT NULL, " +
                    "`variance_count` INTEGER NOT NULL, " +
                    "`variance_value` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))"
            )
            db.execSQL("ALTER TABLE `stock_deltas` ADD COLUMN `count_session_id` TEXT")
            db.execSQL("ALTER TABLE `stock_deltas` ADD COLUMN `actor` TEXT")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_stock_deltas_count_session_id` " +
                    "ON `stock_deltas` (`count_session_id`)"
            )
        }
    }

    /**
     * v4 → v5: data only, no schema change. Products created before opening stock went through
     * the ledger carry a stock figure their movements can't explain (stock 9, movements summing
     * to −3). For each such product, append the missing balance as its earliest movement, dated
     * at the product's creation — so "why does it say 9?" adds up. Stock itself is not touched.
     *
     * A positive gap is the opening stock (PURCHASE). A negative one can't be an opening
     * balance, so it is recorded as a CORRECTION and named as such.
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val gaps = mutableListOf<Triple<String, Int, Long>>()
            db.query(
                "SELECT p.`id`, p.`stock_quantity` - IFNULL(SUM(d.`delta`), 0) AS gap, p.`created_at` " +
                    "FROM `products` p LEFT JOIN `stock_deltas` d ON d.`product_id` = p.`id` " +
                    "GROUP BY p.`id` HAVING gap != 0"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    gaps += Triple(cursor.getString(0), cursor.getInt(1), cursor.getLong(2))
                }
            }
            gaps.forEach { (productId, gap, createdAt) ->
                val opening = gap > 0
                db.execSQL(
                    "INSERT INTO `stock_deltas` (`id`, `product_id`, `delta`, `reason`, `note`, " +
                        "`count_session_id`, `actor`, `created_at`, `synced`) " +
                        "VALUES (?, ?, ?, ?, ?, NULL, NULL, ?, 0)",
                    arrayOf<Any>(
                        UUID.randomUUID().toString(),
                        productId,
                        gap,
                        if (opening) "PURCHASE" else "CORRECTION",
                        if (opening) "Opening stock" else "Balance from before the ledger",
                        createdAt,
                    ),
                )
            }
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "shopmate.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideReceiptCounterDao(db: AppDatabase): ReceiptCounterDao = db.receiptCounterDao()

    @Provides
    fun provideStockDeltaDao(db: AppDatabase): StockDeltaDao = db.stockDeltaDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideSaleDao(db: AppDatabase): SaleDao = db.saleDao()

    @Provides
    fun provideSyncEventDao(db: AppDatabase): SyncEventDao = db.syncEventDao()

    @Provides
    fun provideReportDao(db: AppDatabase): ReportDao = db.reportDao()

    @Provides
    fun provideProductBarcodeDao(db: AppDatabase): ProductBarcodeDao = db.productBarcodeDao()

    @Provides
    fun provideUnknownBarcodeDao(db: AppDatabase): UnknownBarcodeDao = db.unknownBarcodeDao()

    @Provides
    fun provideStockCountDao(db: AppDatabase): StockCountDao = db.stockCountDao()

    @Provides
    fun provideStockInsightDao(db: AppDatabase): StockInsightDao = db.stockInsightDao()
}
