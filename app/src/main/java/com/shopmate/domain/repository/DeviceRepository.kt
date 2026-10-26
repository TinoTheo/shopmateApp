package com.shopmate.domain.repository

import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.Instant

/** What this signed-in device is for (CLAUDE.md § Devices — tills and remote). */
enum class DeviceRole { UNDECIDED, TILL, REMOTE }

/** One of the store's registered tills. */
data class TillInfo(
    val id: String,
    val code: String,
    val name: String,
    val lastSeenAt: Instant,
    val isThisDevice: Boolean,
)

/** Today across the store, for a remote device's home. Sales are counted by till code. */
data class StoreToday(
    val total: BigDecimal,
    val saleCount: Int,
    val tills: List<TillToday>,
) {
    data class TillToday(val name: String, val code: String, val lastSeenAt: Instant, val salesToday: Int, val totalToday: BigDecimal)
}

interface DeviceRepository {
    /** For the active store. UNDECIDED until the owner answers once on this device. */
    val role: Flow<DeviceRole>

    /** True once the server has said this till was removed. It stops syncing until re-registered. */
    val removed: Flow<Boolean>

    /** True when this device has sold before — "Yes, sell here" is then preselected. */
    suspend fun hasSold(): Boolean

    /**
     * Register this device as a till. Offline, it becomes a till now and registers on its next
     * sync. Refused past the plan's till limit.
     */
    suspend fun becomeTill(name: String): TenancyResult<Unit>

    /** Remote management. Refused while this device holds anything not yet sent. */
    suspend fun becomeRemote(): TenancyResult<Unit>

    /**
     * A till's sync heartbeat: registers a till still pending, and learns whether it was
     * removed. False means stop — this device may no longer sync as a till.
     */
    suspend fun heartbeat(storeId: String): Boolean

    suspend fun tills(): TenancyResult<List<TillInfo>>

    suspend fun removeTill(tillId: String): TenancyResult<Unit>

    suspend fun storeToday(): TenancyResult<StoreToday>
}
