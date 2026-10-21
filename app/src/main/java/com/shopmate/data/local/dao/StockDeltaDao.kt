package com.shopmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.shopmate.data.local.entity.StockDeltaEntity
import kotlinx.coroutines.flow.Flow

/** Aggregated net movement per product over a window, used for the 7-day trend indicator. */
data class ProductNetDelta(val productId: String, val net: Int)

@Dao
interface StockDeltaDao {

    @Query("SELECT * FROM stock_deltas WHERE product_id = :productId ORDER BY created_at DESC")
    fun getByProductId(productId: String): Flow<List<StockDeltaEntity>>

    @Query("SELECT * FROM stock_deltas ORDER BY created_at DESC")
    fun getAll(): Flow<List<StockDeltaEntity>>

    @Query("SELECT * FROM stock_deltas ORDER BY created_at DESC LIMIT :limit")
    fun getRecentDeltas(limit: Int = 50): Flow<List<StockDeltaEntity>>

    @Insert
    suspend fun insert(delta: StockDeltaEntity)

    @Query("SELECT * FROM stock_deltas WHERE synced = 0 ORDER BY created_at ASC")
    suspend fun getUnsynced(): List<StockDeltaEntity>

    @Query("UPDATE stock_deltas SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT * FROM stock_deltas WHERE created_at BETWEEN :start AND :end ORDER BY created_at DESC")
    fun getDeltasByDateRange(start: Long, end: Long): Flow<List<StockDeltaEntity>>

    @Query(
        "SELECT product_id AS productId, SUM(delta) AS net FROM stock_deltas " +
            "WHERE created_at >= :since GROUP BY product_id"
    )
    fun getNetDeltasSince(since: Long): Flow<List<ProductNetDelta>>
}
