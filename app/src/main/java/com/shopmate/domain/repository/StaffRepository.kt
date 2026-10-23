package com.shopmate.domain.repository

enum class StaffRole { OWNER, MANAGER, CASHIER }

/** One member of the organisation, with the stores they can work in. */
data class StaffMember(
    val membershipId: String,
    val userId: String,
    val email: String,
    val role: StaffRole,
    val stores: List<StaffStore>,
) {
    data class StaffStore(val storeId: String, val name: String, val canManageStock: Boolean)
}

/** A code that lets one person join, once, within its week. */
data class Invite(val code: String, val role: StaffRole, val storeName: String)

/**
 * Add staff by invite code (CLAUDE.md § Tenancy). Owners invite managers and cashiers; managers
 * invite cashiers. Owners change roles, grant stock rights and remove people.
 */
interface StaffRepository {
    /** Everyone in the organisation. Empty for a cashier, who can't see the list. */
    suspend fun list(): TenancyResult<List<StaffMember>>

    suspend fun invite(storeId: String, storeName: String, role: StaffRole, canManageStock: Boolean): TenancyResult<Invite>

    suspend fun changeRole(membershipId: String, role: StaffRole): TenancyResult<Unit>

    suspend fun setStockRights(membershipId: String, storeId: String, granted: Boolean): TenancyResult<Unit>

    suspend fun remove(membershipId: String): TenancyResult<Unit>
}
