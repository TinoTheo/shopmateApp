package com.shopmate.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shopmate.data.auth.StoreContext
import com.shopmate.domain.model.Licence
import com.shopmate.domain.model.LicencePlan
import com.shopmate.domain.repository.LicenceRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenceRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val dataStore: DataStore<Preferences>,
    private val storeContext: StoreContext,
) : LicenceRepository {

    /** The last licence read for the active store; a cached one for another store is ignored. */
    override val licence: Flow<Licence?> = dataStore.data.map { prefs ->
        if (prefs[FOR_STORE] == null || prefs[FOR_STORE] != prefs[StoreContext.ACTIVE_STORE_ID_KEY]) {
            return@map null
        }
        Licence(
            plan = prefs[PLAN]?.let { runCatching { LicencePlan.valueOf(it) }.getOrNull() } ?: return@map null,
            expiresAt = Instant.ofEpochMilli(prefs[EXPIRES] ?: return@map null),
            graceEndsAt = Instant.ofEpochMilli(prefs[GRACE_ENDS] ?: return@map null),
            maxStores = prefs[MAX_STORES] ?: 1,
            maxStaff = prefs[MAX_STAFF] ?: 0,
            storesUsed = prefs[STORES_USED] ?: 0,
            staffUsed = prefs[STAFF_USED] ?: 0,
        )
    }

    override suspend fun refresh() {
        val storeId = storeContext.getActiveStoreId() ?: return
        val row = runCatching {
            postgrest.rpc(
                function = "current_licence",
                parameters = buildJsonObject { put("p_store_id", storeId) },
            ).decodeList<LicenceRowDto>().firstOrNull()
        }.getOrElse { return } ?: return // offline or no licence row: keep the last answer

        dataStore.edit {
            it[FOR_STORE] = storeId
            it[PLAN] = row.plan.uppercase()
            it[EXPIRES] = row.expiresAt.toEpochMillis()
            it[GRACE_ENDS] = row.graceEndsAt.toEpochMillis()
            it[MAX_STORES] = row.maxStores
            it[MAX_STAFF] = row.maxStaff
            it[STORES_USED] = row.storesUsed
            it[STAFF_USED] = row.staffUsed
        }
    }

    private fun String.toEpochMillis(): Long = OffsetDateTime.parse(this).toInstant().toEpochMilli()

    @Serializable
    private data class LicenceRowDto(
        @SerialName("plan") val plan: String,
        @SerialName("expires_at") val expiresAt: String,
        @SerialName("grace_ends_at") val graceEndsAt: String,
        @SerialName("max_stores") val maxStores: Int,
        @SerialName("max_staff") val maxStaff: Int,
        @SerialName("stores_used") val storesUsed: Int,
        @SerialName("staff_used") val staffUsed: Int,
    )

    private companion object {
        val FOR_STORE = stringPreferencesKey("licence_for_store")
        val PLAN = stringPreferencesKey("licence_plan")
        val EXPIRES = longPreferencesKey("licence_expires_at")
        val GRACE_ENDS = longPreferencesKey("licence_grace_ends_at")
        val MAX_STORES = intPreferencesKey("licence_max_stores")
        val MAX_STAFF = intPreferencesKey("licence_max_staff")
        val STORES_USED = intPreferencesKey("licence_stores_used")
        val STAFF_USED = intPreferencesKey("licence_staff_used")
    }
}
