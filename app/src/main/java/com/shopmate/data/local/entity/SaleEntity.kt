package com.shopmate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shopmate.domain.model.PaymentMethod
import java.math.BigDecimal
import java.time.Instant

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "receipt_number") val receiptNumber: String,
    @ColumnInfo(name = "total_amount") val totalAmount: BigDecimal,
    @ColumnInfo(name = "payment_method") val paymentMethod: PaymentMethod,
    @ColumnInfo(name = "cashier_name") val cashierName: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "synced_at") val syncedAt: Instant?,
)
