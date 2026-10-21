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

    @Query("UPDATE sync_events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE sync_events SET failed = 1, error_message = :errorMessage WHERE id = :id")
    suspend fun markFailed(id: String, errorMessage: String)

    @Query("SELECT * FROM sync_events ORDER BY created_at DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SyncEventEntity>>

    /** Re-queue failed events (rule 7: never delete rows — flip the flag so the worker retries). */
    @Query("UPDATE sync_events SET failed = 0, error_message = NULL WHERE failed = 1")
    suspend fun retryFailed()
}
