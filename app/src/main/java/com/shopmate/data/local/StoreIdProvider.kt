package com.shopmate.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates a stable store_id on first launch and reuses it forever. The id is only sent in
 * sync request bodies — it is not a per-row column, so the local schema is untouched. Multi-store
 * support (later) will let a merchant register additional store ids.
 */
@Singleton
class StoreIdProvider @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun getStoreId(): String {
        val existing = dataStore.data.first()[STORE_ID_KEY]
        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()
        dataStore.edit { it[STORE_ID_KEY] = newId }
        return newId
    }

    companion object {
        val STORE_ID_KEY = stringPreferencesKey("store_id")
    }
}
