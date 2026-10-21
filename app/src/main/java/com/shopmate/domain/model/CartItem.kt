package com.shopmate.domain.model

import java.math.BigDecimal

/**
 * A single line in the in-memory POS cart. Lives only in [PosViewModel] memory and is
 * intentionally never persisted to Room — a killed transaction loses the cart by design.
 */
data class CartItem(
    val product: Product,
    val quantity: Int,
    val lineTotal: BigDecimal, // unitPrice * quantity
    /** Cashier-entered price for this line (loose goods) — overrides the catalog price. */
    val priceOverride: BigDecimal? = null,
) {
    val unitPrice: BigDecimal get() = priceOverride ?: product.sellingPrice
}