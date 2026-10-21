package com.shopmate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sale_id")],
)
data class SaleItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "sale_id") val saleId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "unit_price") val unitPrice: BigDecimal,
    val quantity: BigDecimal,
    @ColumnInfo(name = "line_total") val lineTotal: BigDecimal,
)
