package com.shopmate.domain.repository

import com.shopmate.domain.model.StockCount
import kotlinx.coroutines.flow.Flow

/** One counted line, as the session screen holds it before commit. */
data class CountedLine(
    val productId: String,
    val expected: Int,
    val counted: Int,
) {
    /** Signed: negative means the shelf came up short of the ledger. */
    val variance: Int get() = counted - expected
    val matches: Boolean get() = variance == 0
}

interface StockCountRepository {

    fun recentCounts(limit: Int = 10): Flow<List<StockCount>>

    /**
     * Commits a stocktake as a **batch of signed deltas, never a set of writes**
     * (architecture rule 5). Stock is never assigned; it is always summed.
     *
     * Returns the committed session.
     */
    suspend fun commit(startedAt: java.time.Instant, lines: List<CountedLine>): StockCount
}
