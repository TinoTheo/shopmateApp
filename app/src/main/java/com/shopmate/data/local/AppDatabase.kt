package com.shopmate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shopmate.data.local.converter.Converters
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
import com.shopmate.data.local.entity.CategoryEntity
import com.shopmate.data.local.entity.ProductBarcodeEntity
import com.shopmate.data.local.entity.ProductEntity
import com.shopmate.data.local.entity.ReceiptCounterEntity
import com.shopmate.data.local.entity.SaleEntity
import com.shopmate.data.local.entity.SaleItemEntity
import com.shopmate.data.local.entity.StockCountEntity
import com.shopmate.data.local.entity.StockDeltaEntity
import com.shopmate.data.local.entity.SyncEventEntity
import com.shopmate.data.local.entity.UnknownBarcodeEntity

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        SyncEventEntity::class,
        ReceiptCounterEntity::class,
        StockDeltaEntity::class,
        ProductBarcodeEntity::class,
        UnknownBarcodeEntity::class,
        StockCountEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleDao(): SaleDao
    abstract fun syncEventDao(): SyncEventDao
    abstract fun receiptCounterDao(): ReceiptCounterDao
    abstract fun stockDeltaDao(): StockDeltaDao
    abstract fun reportDao(): ReportDao
    abstract fun productBarcodeDao(): ProductBarcodeDao
    abstract fun unknownBarcodeDao(): UnknownBarcodeDao
    abstract fun stockCountDao(): StockCountDao
    abstract fun stockInsightDao(): StockInsightDao
}
