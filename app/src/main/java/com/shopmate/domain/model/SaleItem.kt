package com.shopmate.domain.model

import java.math.BigDecimal

data class SaleItem(
    val id: String,
    val productId: String,
    val productName: String,
    val categoryName: String?,
    val unitPrice: BigDecimal,
    val quantity: BigDecimal,
    val lineTotal: BigDecimal,
)
