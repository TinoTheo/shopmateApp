package com.shopmate.domain.repository

import com.shopmate.domain.model.ProductInsight
import kotlinx.coroutines.flow.Flow

/**
 * Read-only and derived (architecture rule 6). Nothing here is ever written back to `products`.
 */
interface StockInsightRepository {
    /** Every product paired with its computed velocity, cover, margin and confidence. */
    fun getInsights(): Flow<List<ProductInsight>>

    fun getInsight(productId: String): Flow<ProductInsight?>
}
