package com.shopmate.domain.model

import java.math.BigDecimal
import java.time.Instant

data class Product(
    val id: String,
    val name: String,
    val sku: String,
    val barcode: String?,
    val categoryId: String?,
    val sellingPrice: BigDecimal,
    val costPrice: BigDecimal,
    val previousCostPrice: BigDecimal? = null,
    val costPriceChangedAt: Instant? = null,
    val sellingPriceSetAt: Instant? = null,
    val supplier: String? = null,
    val stockQuantity: Int,
    val lowStockThreshold: Int,
    val unit: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
