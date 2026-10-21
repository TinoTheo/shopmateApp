package com.shopmate.domain.model

import java.time.Instant

data class StockDelta(
    val id: String,
    val productId: String,
    val delta: Int,
    val reason: DeltaReason,
    val note: String?,
    /** Set when [reason] is COUNT. */
    val countSessionId: String? = null,
    /**
     * Till or person. Null until auth ships — a ledger that claims a name it does not know is
     * worse than one that omits it (CLAUDE.md § Schema debt).
     */
    val actor: String? = null,
    val createdAt: Instant,
    val synced: Boolean = false,
)

/** A stock movement paired with its product's display name, for history/activity feeds. */
data class StockDeltaWithProduct(
    val delta: StockDelta,
    val productName: String,
)
