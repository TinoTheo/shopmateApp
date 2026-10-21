package com.shopmate.domain.repository

import com.shopmate.domain.model.DeltaReason
import com.shopmate.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAll(): Flow<List<Product>>
    suspend fun getById(id: String): Product?
    suspend fun add(product: Product)
    suspend fun update(product: Product)
    /**
     * The only way stock ever moves: a signed delta, never an absolute overwrite
     * (architecture rule 4). [countSessionId] ties the movement to a stocktake when
     * [reason] is [DeltaReason.COUNT].
     */
    suspend fun adjustStock(
        productId: String,
        delta: Int,
        reason: DeltaReason,
        note: String? = null,
        countSessionId: String? = null,
    )
    fun search(query: String): Flow<List<Product>>
}
