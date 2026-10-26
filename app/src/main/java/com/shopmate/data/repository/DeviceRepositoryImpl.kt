package com.shopmate.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shopmate.data.auth.StoreContext
import com.shopmate.data.local.TillCode
import com.shopmate.data.local.dao.SaleDao
import com.shopmate.data.local.dao.StockDeltaDao
import com.shopmate.data.local.dao.SyncEventDao
import com.shopmate.data.remote.tenancyRefusal
import com.shopmate.domain.repository.DeviceRepository
import com.shopmate.domain.repository.DeviceRole
import com.shopmate.domain.repository.StoreToday
import com.shopmate.domain.repository.TenancyRefusal
import com.shopmate.domain.repository.TenancyResult
import com.shopmate.domain.repository.TillInfo
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CLAUDE.md § Devices — tills and remote. The role is kept per store on the device: moving the
 * device to another store asks again. A till registers in the store's `tills` by its till code;
 * a remote device never appears there.
 */
@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val dataStore: DataStore<Preferences>,
    private val storeContext: StoreContext,
    private val tillCode: TillCode,
    private val saleDao: SaleDao,
    private val syncEventDao: SyncEventDao,
    private val stockDeltaDao: StockDeltaDao,
) : DeviceRepository {

    override val role: Flow<DeviceRole> = dataStore.data.map { prefs ->
        val forStore = prefs[ROLE_STORE]
        if (forStore == null || forStore != prefs[StoreContext.ACTIVE_STORE_ID_KEY]) {
            DeviceRole.UNDECIDED
        } else {
            prefs[ROLE]?.let { runCatching { DeviceRole.valueOf(it) }.getOrNull() } ?: DeviceRole.UNDECIDED
        }
    }

    override val removed: Flow<Boolean> = dataStore.data.map { it[REMOVED] == true }

    override suspend fun hasSold(): Boolean = saleDao.count() > 0

    override suspend fun becomeTill(name: String): TenancyResult<Unit> {
        val storeId = storeContext.getActiveStoreId() ?: return TenancyResult.Refused(TenancyRefusal.OTHER)
        val registered = runCatching { register(storeId, name) }
        val refusal = registered.exceptionOrNull()?.tenancyRefusal()
        if (refusal != null && refusal != TenancyRefusal.OFFLINE) return TenancyResult.Refused(refusal)
        dataStore.edit {
            it[ROLE_STORE] = storeId
            it[ROLE] = DeviceRole.TILL.name
            it[TILL_NAME] = name.trim()
            it[REMOVED] = false
            // Offline: a till now, registered with its first sync.
            it[PENDING_REGISTRATION] = refusal == TenancyRefusal.OFFLINE
        }
        return TenancyResult.Done(Unit)
    }

    override suspend fun becomeRemote(): TenancyResult<Unit> {
        val storeId = storeContext.getActiveStoreId() ?: return TenancyResult.Refused(TenancyRefusal.OTHER)
        val unsent = syncEventDao.getPendingCount().first() + syncEventDao.getFailedCount().first() +
            stockDeltaDao.getUnsynced().size
        if (unsent > 0) return TenancyResult.Refused(TenancyRefusal.NOT_SENT)
        dataStore.edit {
            it[ROLE_STORE] = storeId
            it[ROLE] = DeviceRole.REMOTE.name
            it[PENDING_REGISTRATION] = false
        }
        return TenancyResult.Done(Unit)
    }

    override suspend fun heartbeat(storeId: String): Boolean {
        val prefs = dataStore.data.first()
        if (prefs[PENDING_REGISTRATION] == true) {
            val result = runCatching { register(storeId, prefs[TILL_NAME] ?: DEFAULT_NAME) }
            if (result.isSuccess) dataStore.edit { it[PENDING_REGISTRATION] = false }
            // A refusal (over the plan's limit) leaves it pending; selling is never blocked by it.
            return true
        }
        val status = runCatching {
            postgrest.rpc(
                function = "till_seen",
                parameters = buildJsonObject {
                    put("p_store_id", storeId)
                    put("p_code", tillCode.get())
                },
            ).decodeAs<String>()
        }.getOrElse { return true } // offline, or the server doesn't know tills yet: carry on
        return when (status) {
            "revoked" -> {
                dataStore.edit { it[REMOVED] = true }
                false
            }
            "unknown" -> {
                // A till from before the register existed: it registers itself now.
                runCatching { register(storeId, prefs[TILL_NAME] ?: DEFAULT_NAME) }
                true
            }
            else -> true
        }
    }

    override suspend fun tills(): TenancyResult<List<TillInfo>> = attempt {
        val storeId = storeContext.getActiveStoreId() ?: error("no store")
        val mine = tillCode.get()
        postgrest.from("tills").select {
            filter {
                eq("store_id", storeId)
                exact("revoked_at", null)
            }
        }.decodeList<TillRowDto>()
            .map { TillInfo(it.id, it.code, it.name, it.lastSeenAt.toInstant(), it.code == mine) }
            .sortedWith(compareBy({ !it.isThisDevice }, { it.name }))
    }

    override suspend fun removeTill(tillId: String): TenancyResult<Unit> = attempt {
        postgrest.from("tills").update(buildJsonObject { put("revoked_at", Instant.now().toString()) }) {
            filter { eq("id", tillId) }
        }
        Unit
    }

    override suspend fun storeToday(): TenancyResult<StoreToday> = attempt {
        val storeId = storeContext.getActiveStoreId() ?: error("no store")
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
        val sales = postgrest.from("sales").select(Columns.raw("receipt_number,total_amount::text")) {
            filter {
                eq("store_id", storeId)
                gte("created_at", startOfDay)
            }
        }.decodeList<SaleTodayDto>()
        val tills = postgrest.from("tills").select {
            filter {
                eq("store_id", storeId)
                exact("revoked_at", null)
            }
        }.decodeList<TillRowDto>()
        // Receipt numbers carry the till code: SM-YYYYMMDD-TTT-XXXX.
        val byCode = sales.groupBy { it.receiptNumber.split('-').getOrNull(2).orEmpty() }
        StoreToday(
            total = sales.fold(BigDecimal.ZERO) { acc, s -> acc + BigDecimal(s.totalAmount) },
            saleCount = sales.size,
            tills = tills.map { till ->
                val mine = byCode[till.code].orEmpty()
                StoreToday.TillToday(
                    name = till.name,
                    code = till.code,
                    lastSeenAt = till.lastSeenAt.toInstant(),
                    salesToday = mine.size,
                    totalToday = mine.fold(BigDecimal.ZERO) { acc, s -> acc + BigDecimal(s.totalAmount) },
                )
            }.sortedByDescending { it.lastSeenAt },
        )
    }

    private suspend fun register(storeId: String, name: String) {
        postgrest.rpc(
            function = "register_till",
            parameters = buildJsonObject {
                put("p_store_id", storeId)
                put("p_code", tillCode.get())
                put("p_name", name.trim().ifBlank { DEFAULT_NAME })
            },
        )
    }

    private inline fun <T> attempt(block: () -> T): TenancyResult<T> =
        runCatching(block).fold(
            onSuccess = { TenancyResult.Done(it) },
            onFailure = { TenancyResult.Refused(it.tenancyRefusal()) },
        )

    private fun String.toInstant(): Instant = OffsetDateTime.parse(this).toInstant()

    @Serializable
    private data class TillRowDto(
        @SerialName("id") val id: String,
        @SerialName("code") val code: String,
        @SerialName("name") val name: String,
        @SerialName("last_seen_at") val lastSeenAt: String,
    )

    @Serializable
    private data class SaleTodayDto(
        @SerialName("receipt_number") val receiptNumber: String,
        @SerialName("total_amount") val totalAmount: String,
    )

    private companion object {
        const val DEFAULT_NAME = "Till"
        val ROLE = stringPreferencesKey("device_role")
        val ROLE_STORE = stringPreferencesKey("device_role_store")
        val TILL_NAME = stringPreferencesKey("till_name")
        val PENDING_REGISTRATION = booleanPreferencesKey("till_pending_registration")
        val REMOVED = booleanPreferencesKey("till_removed")
    }
}
