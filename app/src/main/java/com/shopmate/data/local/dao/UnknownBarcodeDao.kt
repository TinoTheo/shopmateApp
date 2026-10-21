package com.shopmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shopmate.data.local.entity.UnknownBarcodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnknownBarcodeDao {

    @Query("SELECT * FROM unknown_barcodes ORDER BY last_seen_at DESC")
    fun getAll(): Flow<List<UnknownBarcodeEntity>>

    @Query("SELECT COUNT(*) FROM unknown_barcodes")
    fun countAll(): Flow<Int>

    @Query("SELECT * FROM unknown_barcodes WHERE code = :code LIMIT 1")
    suspend fun findByCode(code: String): UnknownBarcodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UnknownBarcodeEntity)

    @Query("DELETE FROM unknown_barcodes WHERE code = :code")
    suspend fun deleteByCode(code: String)
}
