package com.shopmate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Someone who can unlock this till with a PIN — a copy of the store's `staff` rows, pulled on
 * sync so the lock works offline. The PIN is only ever a PBKDF2 hash and its salt.
 */
@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** `owner`, `manager` or `cashier`, as on the server. */
    val role: String,
    @ColumnInfo(name = "pin_hash") val pinHash: String,
    @ColumnInfo(name = "pin_salt") val pinSalt: String,
    @ColumnInfo(name = "can_manage_stock") val canManageStock: Boolean,
    /** Set on the owner's own row. */
    @ColumnInfo(name = "user_id") val userId: String?,
    val active: Boolean,
)
