package com.shopmate.domain.repository

import com.shopmate.domain.model.Sale
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SaleRepository {
    suspend fun recordSale(sale: Sale)

    /**
     * Gives a sale a fresh receipt number from this till, keeping its date, and moves its ledger
     * notes with it. Only for a sale the server refused because another sale in the store already
     * has its number — receipts issued before receipt numbers carried a till code. Returns the
     * renumbered sale, or null if it no longer exists.
     */
    suspend fun renumber(saleId: String): Sale?
    fun getSalesByDateRange(start: Instant, end: Instant): Flow<List<Sale>>
    fun getRecentSales(limit: Int): Flow<List<Sale>>
}
