package com.shopmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.shopmate.data.local.entity.ReceiptCounterEntity

@Dao
interface ReceiptCounterDao {

    @Query("SELECT last_number FROM receipt_counter WHERE date = :date")
    suspend fun getLastNumber(date: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(counter: ReceiptCounterEntity)

    /**
     * Atomically reads today's counter, increments it, persists it and returns the new value.
     * Runs in a single transaction so concurrent sales on one device cannot reuse a number.
     */
    @Transaction
    suspend fun nextNumber(date: String): Int {
        val next = (getLastNumber(date) ?: 0) + 1
        upsert(ReceiptCounterEntity(date = date, lastNumber = next))
        return next
    }
}