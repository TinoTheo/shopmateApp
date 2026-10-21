package com.shopmate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "stock_counts")
data class StockCountEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "committed_at") val committedAt: Long?,
    @ColumnInfo(name = "counted_by") val countedBy: String?,
    @ColumnInfo(name = "item_count") val itemCount: Int,
    @ColumnInfo(name = "variance_count") val varianceCount: Int,
    @ColumnInfo(name = "variance_value") val varianceValue: BigDecimal,
)
