package com.shopmate.domain.repository

import com.shopmate.domain.model.Sale
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SaleRepository {
    suspend fun recordSale(sale: Sale)
    fun getSalesByDateRange(start: Instant, end: Instant): Flow<List<Sale>>
    fun getRecentSales(limit: Int): Flow<List<Sale>>
}
