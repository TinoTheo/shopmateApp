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

    suspend fun setActiveStoreId(storeId: String) {
        dataStore.edit { it[ACTIVE_STORE_ID_KEY] = storeId }
    }

    suspend fun clearActiveStore() {
        dataStore.edit { it.remove(ACTIVE_STORE_ID_KEY) }
    }

    /** First-time owner setup — server-side RPC creates org + store atomically. */
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
        setActiveStoreId(storeId)
        return storeId
    }

    companion object {
        val ACTIVE_STORE_ID_KEY = stringPreferencesKey("active_store_id")
    }
}
