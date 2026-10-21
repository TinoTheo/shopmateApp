package com.shopmate.domain.model

import java.math.BigDecimal
import java.time.Instant

data class Sale(
    val id: String,
    val receiptNumber: String,
    val totalAmount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val cashierName: String,
    val items: List<SaleItem>,
    val createdAt: Instant,
    val syncedAt: Instant?,
)
