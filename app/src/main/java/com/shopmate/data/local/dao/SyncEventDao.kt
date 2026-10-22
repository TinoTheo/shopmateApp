package com.shopmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shopmate.data.local.entity.SyncEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SyncEventEntity)

    @Query("SELECT * FROM sync_events WHERE synced = 0 AND failed = 0 ORDER BY created_at ASC")
    fun getPending(): Flow<List<SyncEventEntity>>

    @Query("SELECT COUNT(*) FROM sync_events WHERE synced = 0 AND failed = 0")
    fun getPendingCount(): Flow<Int>

    /** Uploads the server refused. Never zero just because nothing is waiting. */
    @Query("SELECT COUNT(*) FROM sync_events WHERE failed = 1")
    fun getFailedCount(): Flow<Int>

    @Query("UPDATE sync_events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE sync_events SET failed = 1, error_message = :errorMessage WHERE id = :id")
    suspend fun markFailed(id: String, errorMessage: String)

    @Query("SELECT * FROM sync_events ORDER BY created_at DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SyncEventEntity>>

    /** Re-queue failed events (rule 7: never delete rows — flip the flag so the worker retries). */
    /** History rows with the sale each SALE event carries, for "Sale · $3.40". */
    @Query(
        """
        SELECT e.id, e.event_type, e.payload, e.created_at, e.synced, e.failed, e.error_message,
               s.receipt_number, s.total_amount, s.original_receipt_number, s.synced_at AS sale_synced_at
        FROM sync_events e
        LEFT JOIN sales s ON e.event_type = 'SALE' AND s.id = e.payload
        ORDER BY e.created_at DESC
        LIMIT :limit
        """
    )
    fun getRecentWithSale(limit: Int): Flow<List<SyncEventWithSaleRow>>

    /** Puts one refused upload back in the queue. */
    @Query("UPDATE sync_events SET failed = 0, error_message = NULL WHERE id = :id")
    suspend fun retry(id: String)

    @Query("UPDATE sync_events SET failed = 0, error_message = NULL WHERE failed = 1")
    suspend fun retryFailed()
}
