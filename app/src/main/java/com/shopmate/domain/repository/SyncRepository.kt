package com.shopmate.domain.repository

import com.shopmate.domain.model.Sale
import com.shopmate.domain.model.SyncEvent
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    fun getPendingCount(): Flow<Int>
    fun getPendingEvents(): Flow<List<SyncEvent>>
    fun getRecentEvents(limit: Int = 50): Flow<List<SyncEvent>>
    suspend fun markSynced(id: String)
    suspend fun markFailed(id: String, errorMessage: String)
    suspend fun queueSaleEvent(sale: Sale)
    suspend fun queueSnapshotEvent()
}
