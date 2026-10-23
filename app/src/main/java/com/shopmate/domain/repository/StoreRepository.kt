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

/**
 * One store per device (CLAUDE.md § Tenancy). Room holds one store's data at a time, so moving
 * this till to another store sends everything first, then starts clean.
 */
interface StoreRepository {
    suspend fun accessibleStores(): List<StoreChoice>

    suspend fun switchTo(store: StoreChoice): StoreSwitch
}
