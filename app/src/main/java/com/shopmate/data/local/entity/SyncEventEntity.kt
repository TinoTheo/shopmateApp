package com.shopmate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_events")
data class SyncEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "event_type") val eventType: String,
    val payload: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val synced: Boolean = false,
    val failed: Boolean = false,
    @ColumnInfo(name = "error_message") val errorMessage: String?,
)
