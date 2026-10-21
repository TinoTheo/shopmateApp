package com.shopmate.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the dashboard API token in DataStore. The token is entered in Settings (Phase 7);
 * until one exists, all sync workers no-op so the app stays fully usable offline.
 */
@Singleton
class DataStoreAuthTokenProvider @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AuthTokenProvider {

    override suspend fun getToken(): String? =
        dataStore.data.first()[AUTH_TOKEN_KEY]

    override suspend fun hasToken(): Boolean = getToken() != null

    suspend fun saveToken(token: String) {
        dataStore.edit { it[AUTH_TOKEN_KEY] = token }
    }

    suspend fun clearToken() {
        dataStore.edit { it.remove(AUTH_TOKEN_KEY) }
    }

    companion object {
        val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
    }
}
