package com.shopmate.data.local.dao

import androidx.room.ColumnInfo
import java.math.BigDecimal
import java.time.Instant

/** A sync event with the sale it carries (sale columns are null for snapshots). */
data class SyncEventWithSaleRow(
    val id: String,
    @ColumnInfo(name = "event_type") val eventType: String,
    val payload: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val synced: Boolean,
    val failed: Boolean,
    @ColumnInfo(name = "error_message") val errorMessage: String?,
    @ColumnInfo(name = "receipt_number") val receiptNumber: String?,
    @ColumnInfo(name = "total_amount") val totalAmount: BigDecimal?,
    @ColumnInfo(name = "original_receipt_number") val originalReceiptNumber: String?,
    @ColumnInfo(name = "sale_synced_at") val saleSyncedAt: Instant?,
)
