package com.shopmate.domain.model

import java.time.Instant

/**
 * A code scanned at the till that isn't in the catalogue yet.
 *
 * An unknown barcode NEVER blocks a sale — it lands here to be named later
 * (CLAUDE.md § Domain models, architecture rule 11).
 */
data class UnknownBarcode(
    val code: String,
    val hits: Int,
    val firstSeenAt: Instant,
    val lastSeenAt: Instant,
    val lastTill: String?,
)
