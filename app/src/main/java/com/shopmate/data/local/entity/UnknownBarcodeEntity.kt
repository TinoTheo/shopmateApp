package com.shopmate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Codes rung up at the till that aren't in the catalogue yet. The code itself is the key. */
@Entity(tableName = "unknown_barcodes")
data class UnknownBarcodeEntity(
    @PrimaryKey val code: String,
    val hits: Int,
    @ColumnInfo(name = "first_seen_at") val firstSeenAt: Long,
    @ColumnInfo(name = "last_seen_at") val lastSeenAt: Long,
    @ColumnInfo(name = "last_till") val lastTill: String?,
)
