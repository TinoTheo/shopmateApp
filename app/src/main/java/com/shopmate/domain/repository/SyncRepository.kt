package com.shopmate.domain.repository

import com.shopmate.domain.model.Sale
import com.shopmate.domain.model.SyncEntry
import com.shopmate.domain.model.SyncEvent
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SyncRepository {
    fun getPendingCount(): Flow<Int>

    /** Uploads the server refused; they stay on the device until sent again. */
    fun getFailedCount(): Flow<Int>

    /** Puts refused uploads back in the queue (never deletes them — architecture rule 9). */
    suspend fun retryFailed()
    fun getPendingEvents(): Flow<List<SyncEvent>>
    fun getRecentEvents(limit: Int = 50): Flow<List<SyncEvent>>

    /** History rows, newest first, with the sale each one carries. */
    fun getRecentEntries(limit: Int = 50): Flow<List<SyncEntry>>

    /** When a sale last reached the store, or null if none has. */
    fun lastSentAt(): Flow<Instant?>

    /**
     * How recent the other tills' movements on this till are — when it last pulled the store's
     * ledger (§ Multi-till). Null until it has once.
     */
    fun otherTillsUpTo(): Flow<Instant?>

    /** Puts one refused upload back in the queue. */
    suspend fun retry(eventId: String)
    suspend fun markSynced(id: String)
    suspend fun markFailed(id: String, errorMessage: String)
    suspend fun queueSaleEvent(sale: Sale)
    suspend fun queueSnapshotEvent()
}
