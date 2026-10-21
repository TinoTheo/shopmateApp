package com.shopmate.domain.model

import java.time.Instant

/**
 * One product, many codes. The same item arriving from two suppliers with two barcodes is
 * the norm in this market, so the code is a row rather than a column (CLAUDE.md § Domain models).
 */
data class ProductBarcode(
    val id: String,
    val productId: String,
    val code: String,
    val createdAt: Instant,
)
