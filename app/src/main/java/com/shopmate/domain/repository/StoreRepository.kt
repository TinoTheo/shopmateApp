package com.shopmate.domain.repository

/** A store the signed-in member can work in. */
data class StoreChoice(val id: String, val name: String)

/** What happened when the till was asked to work for another store. */
sealed interface StoreSwitch {
    data object Switched : StoreSwitch

    /** Something is still on this till only; switching would lose it. Send it first. */
    data class NotSentYet(val count: Int) : StoreSwitch

    /** Switching needs the connection: it must confirm nothing is left to send. */
    data object Offline : StoreSwitch
}

/** Why the server refused a tenancy action, in terms the screen can word. */
enum class TenancyRefusal { STORE_LIMIT, STAFF_LIMIT, TILL_LIMIT, LICENCE_EXPIRED, NOT_ALLOWED, NAME_TAKEN, NOT_SENT, OFFLINE, OTHER }

sealed interface TenancyResult<out T> {
    data class Done<T>(val value: T) : TenancyResult<T>
    data class Refused(val reason: TenancyRefusal) : TenancyResult<Nothing>
}

/**
 * One store per device (CLAUDE.md § Tenancy). Room holds one store's data at a time, so moving
 * this till to another store sends everything first, then starts clean.
 */
interface StoreRepository {
    suspend fun accessibleStores(): List<StoreChoice>

    suspend fun switchTo(store: StoreChoice): StoreSwitch

    /**
     * At sign-in: go back to the store this till's data belongs to when the member can still
     * work there, otherwise their first store. Null when they have none (onboarding).
     */
    suspend fun resolveOnSignIn(): StoreSwitch?

    /** A new owner's organisation, first store and trial licence; moves the till to the store. */
    suspend fun createOrganization(orgName: String, storeName: String, currencySymbol: String): TenancyResult<StoreSwitch>

    /** Adds a store to the organisation. Does not move this till to it. */
    suspend fun createStore(name: String, currencySymbol: String): TenancyResult<StoreChoice>

}
