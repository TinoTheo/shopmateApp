package com.shopmate.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * A stocktake session, so variance can be read as a pattern across sessions rather than as a
 * scatter of one-off corrections (CLAUDE.md § Domain models).
 */
data class StockCount(
    val id: String,
    val startedAt: Instant,
    val committedAt: Instant?,
    val countedBy: String?,
    val itemCount: Int,
    val varianceCount: Int,
    /** Signed; negative means the count came up short. */
    val varianceValue: BigDecimal,
)
