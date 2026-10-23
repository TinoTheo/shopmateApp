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
    /** Set only when the sale was renumbered: the number on the customer's paper receipt. */
    val originalReceiptNumber: String? = null,
    /** The staff member who rang it up (§ Staff); [cashierName] carries their name. */
    val staffId: String? = null,
)
