package com.shopmate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shopmate.domain.model.DeltaReason

@Entity(
    tableName = "stock_deltas",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("product_id"), Index("created_at"), Index("count_session_id")],
)
data class StockDeltaEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "product_id") val productId: String,
    val delta: Int,
    val reason: DeltaReason,
    val note: String?,
    /** Set when [reason] is COUNT — ties the delta to its stocktake session. */
    @ColumnInfo(name = "count_session_id") val countSessionId: String? = null,
    /** Till or person. Stays null until auth ships — never a placeholder. */
    val actor: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val synced: Boolean = false,
)
