package com.shopmate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One product, many codes. `code` is stored already normalised by
 * [BarcodeNormaliser][com.shopmate.domain.util.BarcodeNormaliser] — never write a raw scan here.
 */
@Entity(
    tableName = "product_barcodes",
    indices = [
        Index(value = ["code"], unique = true),
        Index("product_id"),
    ],
)
data class ProductBarcodeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "product_id") val productId: String,
    val code: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
