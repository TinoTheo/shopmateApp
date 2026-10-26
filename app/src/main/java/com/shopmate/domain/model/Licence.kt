package com.shopmate.domain.model

import java.time.Duration
import java.time.Instant

enum class LicencePlan { TRIAL, BASIC, PRO }

/** The organisation's licence as last read from the server, kept for offline use. */
data class Licence(
    val plan: LicencePlan,
    val expiresAt: Instant,
    val graceEndsAt: Instant,
    val maxStores: Int,
    val maxStaff: Int,
    val storesUsed: Int,
    val staffUsed: Int,
) {
    fun state(now: Instant = Instant.now()): LicenceState = when {
        now.isAfter(graceEndsAt) -> LicenceState.Expired
        now.isAfter(expiresAt) -> LicenceState.Grace(daysUntil(graceEndsAt, now))
        now.isAfter(expiresAt.minus(WARNING)) -> LicenceState.Ending(daysUntil(expiresAt, now))
        else -> LicenceState.Active
    }

    val canAddStore: Boolean get() = storesUsed < maxStores
    val canAddStaff: Boolean get() = staffUsed < maxStaff

    private companion object {
        val WARNING: Duration = Duration.ofDays(7)

        /** Whole days left, rounding up: 2 hours left reads as "1 day", never "0 days". */
        fun daysUntil(end: Instant, now: Instant): Int =
            ((Duration.between(now, end).toHours() + 23) / 24).toInt().coerceAtLeast(1)
    }
}

/**
 * CLAUDE.md § Licensing: active → ending (7 days before) → grace (7 days after) → expired.
 * Only [Expired] stops anything, and only new sales; sales already taken always upload.
 */
sealed interface LicenceState {
    data object Active : LicenceState
    data class Ending(val daysLeft: Int) : LicenceState
    data class Grace(val daysLeft: Int) : LicenceState
    data object Expired : LicenceState

    val blocksNewSales: Boolean get() = this is Expired
}
