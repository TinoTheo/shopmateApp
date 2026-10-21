package com.shopmate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sku: String,
    val barcode: String?,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "selling_price") val sellingPrice: BigDecimal,
    @ColumnInfo(name = "cost_price") val costPrice: BigDecimal,
    @ColumnInfo(name = "previous_cost_price") val previousCostPrice: BigDecimal? = null,
    @ColumnInfo(name = "cost_price_changed_at") val costPriceChangedAt: Instant? = null,
    @ColumnInfo(name = "selling_price_set_at") val sellingPriceSetAt: Instant? = null,
    val supplier: String? = null,
    @ColumnInfo(name = "stock_quantity") val stockQuantity: Int,
    @ColumnInfo(name = "low_stock_threshold") val lowStockThreshold: Int,
    val unit: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
