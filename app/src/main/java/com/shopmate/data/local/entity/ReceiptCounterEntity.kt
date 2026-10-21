package com.shopmate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Daily receipt sequence counter. One row per calendar day (local time).
 * [date] is formatted "yyyy-MM-dd"; [lastNumber] is the last sequence issued that day.
 */
@Entity(tableName = "receipt_counter")
data class ReceiptCounterEntity(
    @PrimaryKey val date: String,
    @ColumnInfo(name = "last_number") val lastNumber: Int,
)