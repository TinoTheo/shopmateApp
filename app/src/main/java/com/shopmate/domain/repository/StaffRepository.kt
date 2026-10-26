package com.shopmate.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class StaffRole { OWNER, MANAGER, CASHIER }

/** Someone who unlocks this till with a PIN (CLAUDE.md § Staff). */
data class TillStaff(
    val id: String,
    val name: String,
    val role: StaffRole,
    val canManageStock: Boolean,
) {
    /** Owners and managers always move stock; a cashier only with the owner's grant. */
    val mayMoveStock: Boolean get() = role != StaffRole.CASHIER || canManageStock
}

/**
 * Staff with PINs. The list lives on the server and every till of the store keeps a copy, so the
 * lock works offline. Adding, changing and removing people is the owner's, and needs the
 * connection; unlocking never does.
 */
interface StaffRepository {
    /** Active staff on this till, owner first — the "Who's selling?" list. */
    val staff: Flow<List<TillStaff>>

    /** Who has unlocked the till. Null when locked, or on a till with no staff. */
    val current: StateFlow<TillStaff?>

    /** True when this store has staff and nobody has unlocked the till yet. */
    val locked: Flow<Boolean>

    /** Checks the PIN on the device. True and unlocked when it matches. */
    suspend fun unlock(staffId: String, pin: String): Boolean

    /** Back to "Who's selling?" — the next person taps their name. */
    fun lock()

    /** Replaces this till's copy with the server's list for [storeId]. Quiet when offline. */
    suspend fun pull(storeId: String)

    /** The owner's own till PIN. Set before anyone else is added, so the owner can unlock too. */
    suspend fun setOwnerPin(ownerName: String, pin: String): TenancyResult<Unit>

    suspend fun add(name: String, role: StaffRole, pin: String, canManageStock: Boolean): TenancyResult<Unit>

    suspend fun update(staffId: String, role: StaffRole, canManageStock: Boolean): TenancyResult<Unit>

    suspend fun changePin(staffId: String, pin: String): TenancyResult<Unit>

    /** They can no longer unlock any till; their name stays on the sales they rang up. */
    suspend fun remove(staffId: String): TenancyResult<Unit>
}
