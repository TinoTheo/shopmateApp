package com.shopmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shopmate.data.local.entity.StockCountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockCountDao {

    @Query("SELECT * FROM stock_counts WHERE committed_at IS NOT NULL ORDER BY committed_at DESC")
    fun getCommitted(): Flow<List<StockCountEntity>>

    @Query("SELECT * FROM stock_counts WHERE committed_at IS NOT NULL ORDER BY committed_at DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<StockCountEntity>>

    @Query("SELECT * FROM stock_counts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StockCountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StockCountEntity)
}
