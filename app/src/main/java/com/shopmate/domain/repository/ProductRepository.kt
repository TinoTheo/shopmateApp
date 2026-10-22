package com.shopmate.domain.repository

import com.shopmate.domain.model.DeltaReason
import com.shopmate.domain.model.Product
import kotlinx.coroutines.flow.Flow

/** A non-sale stock movement was attempted by a member without stock rights at this store. */
class StockPermissionRequired : IllegalStateException(
    "Only owners, managers, or cashiers given stock rights can add or adjust stock.",
)

interface ProductRepository {
    fun getAll(): Flow<List<Product>>
    suspend fun getById(id: String): Product?
    /**
     * Inserts [product] at zero stock and, in the same transaction, records [openingStock] as
     * its first ledger movement — so the ledger explains the very first number on the shelf.
     * [Product.stockQuantity] on the argument is ignored.
     */
    suspend fun add(product: Product, openingStock: Int = 0)

    /** Saves everything about [product] except its stock, which only [adjustStock] moves. */
    suspend fun update(product: Product)
    /**
     * The only way stock ever moves: a signed delta, never an absolute overwrite
     * (architecture rule 4). [countSessionId] ties the movement to a stocktake when
     * [reason] is [DeltaReason.COUNT].
     *
     * Any [reason] other than SALE needs [StorePermissionRepository.canManageStock]; without it
     * this throws [StockPermissionRequired] and nothing is written.
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
