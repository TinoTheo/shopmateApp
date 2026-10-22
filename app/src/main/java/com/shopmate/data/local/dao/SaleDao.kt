package com.shopmate.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.shopmate.data.local.entity.SaleEntity
import com.shopmate.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sale_id",
    )
    val items: List<SaleItemEntity>,
)

@Dao
interface SaleDao {

    @Transaction
    suspend fun insertSaleWithItems(sale: SaleEntity, items: List<SaleItemEntity>) {
        insertSale(sale)
        insertItems(items)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Transaction
    @Query("SELECT * FROM sales WHERE created_at BETWEEN :startMs AND :endMs ORDER BY created_at DESC")
    fun getByDateRange(startMs: Long, endMs: Long): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales ORDER BY created_at DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: String): SaleWithItems?

    @Query("UPDATE sales SET synced_at = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("UPDATE sales SET receipt_number = :receiptNumber WHERE id = :id")
    suspend fun updateReceiptNumber(id: String, receiptNumber: String)
}
