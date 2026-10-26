package com.shopmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.shopmate.data.local.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    /** Active staff, owner first, then by name — the order of "Who's selling?". */
    @Query("SELECT * FROM staff WHERE active = 1 ORDER BY role = 'owner' DESC, name COLLATE NOCASE")
    fun observeActive(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun getById(id: String): StaffEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(staff: List<StaffEntity>)

    @Query("DELETE FROM staff")
    suspend fun clear()

    /** The server's list replaces the till's: someone removed there can't unlock here. */
    @Transaction
    suspend fun replaceAll(staff: List<StaffEntity>) {
        clear()
        insertAll(staff)
    }
}
