package com.shopmate.sync

import androidx.room.withTransaction
import com.shopmate.data.local.AppDatabase
import com.shopmate.data.local.dao.CategoryDao
import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.local.dao.StockDeltaDao
import com.shopmate.data.local.entity.CategoryEntity
import com.shopmate.data.local.entity.ProductEntity
import com.shopmate.data.local.entity.StockDeltaEntity
import com.shopmate.data.remote.ShopMateRemote
import com.shopmate.data.repository.ProductRepositoryImpl
import com.shopmate.domain.model.DeltaReason
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fills an empty till with its store's catalogue (CLAUDE.md § Tenancy — one store per device):
 * a new staff phone, or a till just moved to another store, can sell on its first sync.
 *
 * The source is the store's latest snapshot — the only place a till's catalogue reaches the
 * server. Each product's stock arrives as an "Opening stock" movement marked synced: the server
 * already counts that stock, so it never uploads again, and the ledger still explains the number.
 *
 * Only an empty till is filled. A till with products owns its catalogue; filling it again would
 * bring back products deleted on it.
 */
@Singleton
class CatalogueLoader @Inject constructor(
    private val db: AppDatabase,
    private val remote: ShopMateRemote,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val stockDeltaDao: StockDeltaDao,
) {

    /** Returns how many products were loaded; 0 when the till already had some. */
    suspend fun loadIfEmpty(storeId: String): Int {
        if (productDao.count() > 0) return 0
        val snapshot = remote.fetchLatestSnapshot(storeId) ?: return 0
        if (snapshot.storeId != storeId || snapshot.payload.products.isEmpty()) return 0

        val now = Instant.now()
        db.withTransaction {
            // A second sync can race the first; whichever lands second finds products and stops.
            if (productDao.count() > 0) return@withTransaction
            snapshot.payload.categories.forEach { category ->
                categoryDao.insert(CategoryEntity(category.id, category.name, category.colour ?: DEFAULT_COLOUR))
            }
            snapshot.payload.products.forEach { p ->
                val stock = p.stockQuantity.coerceAtLeast(0)
                productDao.insert(
                    ProductEntity(
                        id = p.id,
                        name = p.name,
                        sku = p.sku.orEmpty(),
                        barcode = null,
                        categoryId = p.categoryId,
                        sellingPrice = BigDecimal(p.sellingPrice),
                        costPrice = p.costPrice?.let(::BigDecimal) ?: BigDecimal.ZERO,
                        stockQuantity = stock,
                        lowStockThreshold = p.lowStockThreshold,
                        unit = p.unit,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                if (stock > 0) {
                    stockDeltaDao.insert(
                        StockDeltaEntity(
                            id = UUID.randomUUID().toString(),
                            productId = p.id,
                            delta = stock,
                            reason = DeltaReason.PURCHASE,
                            note = ProductRepositoryImpl.OPENING_STOCK_NOTE,
                            createdAt = now.toEpochMilli(),
                            synced = true,
                        ),
                    )
                }
            }
        }
        return snapshot.payload.products.size
    }

    private companion object {
        const val DEFAULT_COLOUR = "#005B52"
    }
}
