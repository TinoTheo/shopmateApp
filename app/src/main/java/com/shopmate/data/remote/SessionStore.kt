package com.shopmate.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the Supabase session in DataStore so a shop that opens offline stays signed in
 * (CLAUDE.md § Backend & sync). Installed as the Auth plugin's [SessionManager], so supabase-kt
 * reads and writes it on sign-in, refresh and sign-out.
 *
 * Also carries the "server rejected this session" flag: when a sync upload comes back `401`, the
 * worker stops draining and marks the session rejected rather than failing the queued events.
 * Any fresh session (sign-in or successful refresh) clears it.
 */
@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SessionManager {

    private val json = Json { ignoreUnknownKeys = true }

    /** True after the server answered `401` to a sync upload; false once a new session lands. */
    val rejected: Flow<Boolean> = dataStore.data.map { it[REJECTED_KEY] ?: false }

    override suspend fun saveSession(session: UserSession) {
        val encoded = json.encodeToString(UserSession.serializer(), session)
        dataStore.edit {
            it[SESSION_KEY] = encoded
            it.remove(REJECTED_KEY)
        }
    }

    override suspend fun loadSession(): UserSession? {
        val encoded = dataStore.data.first()[SESSION_KEY] ?: return null
        // A session written by an older supabase-kt that no longer decodes is treated as absent:
        // the cashier signs in again, the queue is untouched.
        return runCatching { json.decodeFromString(UserSession.serializer(), encoded) }.getOrNull()
    }

    override suspend fun deleteSession() {
        dataStore.edit {
            it.remove(SESSION_KEY)
            it.remove(REJECTED_KEY)
        }
    }

    suspend fun markRejected() {
        dataStore.edit { it[REJECTED_KEY] = true }
    }

    companion object {
        private val SESSION_KEY = stringPreferencesKey("supabase_session")
        private val REJECTED_KEY = booleanPreferencesKey("supabase_session_rejected")
    }
}
