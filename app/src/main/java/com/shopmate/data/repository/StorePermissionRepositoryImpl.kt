package com.shopmate.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shopmate.data.auth.StoreContext
import com.shopmate.domain.repository.StorePermissionRepository
import com.shopmate.domain.repository.StaffRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the server's answer together with the store it was given for. An answer for another
 * store — after a store switch or a sign-out — reads as false until [refresh] confirms the new
 * one, so a stale grant never carries over.
 */
@Singleton
class StorePermissionRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val dataStore: DataStore<Preferences>,
    private val storeContext: StoreContext,
    private val staffRepository: StaffRepository,
) : StorePermissionRepository {

    /**
     * The signed-in account's right (the server's answer) narrowed by the person at the till:
     * a cashier who unlocked it moves stock only with the owner's grant (§ Staff).
     */
    override val canManageStock: Flow<Boolean> =
        combine(storeContext.activeStoreIdFlow, dataStore.data, staffRepository.current) { activeStoreId, prefs, person ->
            activeStoreId != null &&
                prefs[ANSWERED_FOR_STORE_KEY] == activeStoreId &&
                prefs[CAN_MANAGE_STOCK_KEY] == true &&
                (person == null || person.mayMoveStock)
        }

    override suspend fun refresh() {
        val storeId = storeContext.getActiveStoreId() ?: return
        val allowed = runCatching {
            postgrest.rpc(
                function = "auth_can_manage_stock",
                parameters = buildJsonObject { put("p_store_id", storeId) },
            ).decodeAs<Boolean>()
        }.getOrNull() ?: return // offline or refused: keep the last answer
        dataStore.edit {
            it[ANSWERED_FOR_STORE_KEY] = storeId
            it[CAN_MANAGE_STOCK_KEY] = allowed
        }
    }

    companion object {
        private val CAN_MANAGE_STOCK_KEY = booleanPreferencesKey("can_manage_stock")
        private val ANSWERED_FOR_STORE_KEY = stringPreferencesKey("can_manage_stock_store_id")
    }
}
