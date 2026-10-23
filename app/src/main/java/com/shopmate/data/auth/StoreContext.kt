package com.shopmate.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shopmate.data.remote.dto.StoreDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which cloud store this device is acting for. RLS already scopes every query to the stores
 * the signed-in user can access; this class picks ONE of those as active and persists the
 * choice in DataStore. The active id replaces the legacy locally generated store_id in every
 * sync payload.
 */
@Singleton
class StoreContext @Inject constructor(
    private val postgrest: Postgrest,
    private val dataStore: DataStore<Preferences>,
) {

    /** Stores the signed-in user can access — RLS does the filtering server-side. */
    suspend fun fetchAccessibleStores(): List<StoreDto> =
        postgrest.from("stores").select().decodeList<StoreDto>()

    val activeStoreIdFlow: Flow<String?> =
        dataStore.data.map { it[ACTIVE_STORE_ID_KEY] }

    suspend fun getActiveStoreId(): String? =
        dataStore.data.first()[ACTIVE_STORE_ID_KEY]

    /**
     * Also records [storeId] as the store this till's local data belongs to. Callers make that
     * true first — see StoreRepositoryImpl.moveTill, which clears the till before a real move.
     */
    suspend fun setActiveStoreId(storeId: String) {
        dataStore.edit {
            it[ACTIVE_STORE_ID_KEY] = storeId
            it[LOCAL_DATA_STORE_ID_KEY] = storeId
        }
    }

    /**
     * The store this till's Room data belongs to. Kept through sign-out, so the next sign-in can
     * go back to it rather than upload one store's sales as another's. Tills from before this
     * key existed fall back to the active store.
     */
    suspend fun localDataStoreId(): String? {
        val prefs = dataStore.data.first()
        return prefs[LOCAL_DATA_STORE_ID_KEY] ?: prefs[ACTIVE_STORE_ID_KEY]
    }

    suspend fun clearActiveStore() {
        dataStore.edit { it.remove(ACTIVE_STORE_ID_KEY) }
    }

    /**
     * First-time owner setup — server-side RPC creates org + store (and its trial licence)
     * atomically. Returns the store id; StoreRepository moves the till to it.
     */
    suspend fun createOrganizationWithStore(
        orgName: String,
        storeName: String,
        currency: String,
    ): String {
        val storeId = postgrest.rpc(
            function = "create_organization_with_store",
            parameters = buildJsonObject {
                put("org_name", orgName)
                put("store_name", storeName)
                put("currency", currency)
            },
        ).decodeAs<String>()
        return storeId
    }

    companion object {
        val ACTIVE_STORE_ID_KEY = stringPreferencesKey("active_store_id")
        private val LOCAL_DATA_STORE_ID_KEY = stringPreferencesKey("local_data_store_id")
    }
}
