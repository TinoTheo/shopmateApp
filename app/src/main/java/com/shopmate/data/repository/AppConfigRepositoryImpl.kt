package com.shopmate.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shopmate.BuildConfig
import com.shopmate.domain.repository.AppConfig
import com.shopmate.domain.repository.AppConfigRepository
import com.shopmate.domain.util.DashboardAddress
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads `app_config` (readable signed out, written only from the Supabase dashboard) and keeps
 * the last answer in DataStore. A dashboard address off ShopMate's trusted domains is dropped
 * before it is stored, so the guard holds even if the row is changed.
 */
@Singleton
class AppConfigRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val dataStore: DataStore<Preferences>,
) : AppConfigRepository {

    private val guard = DashboardAddress(BuildConfig.DASHBOARD_TRUSTED_DOMAINS)

    override val config: Flow<AppConfig> = dataStore.data.map { prefs ->
        AppConfig(
            dashboardUrl = guard.accept(prefs[DASHBOARD_URL]) ?: guard.accept(BuildConfig.WEB_DASHBOARD_URL),
            supportContact = prefs[SUPPORT]?.takeIf { it.isNotBlank() },
            updateAvailable = (prefs[MIN_VERSION] ?: 0) > BuildConfig.VERSION_CODE,
            notice = prefs[NOTICE]?.takeIf { it.isNotBlank() },
        )
    }

    override suspend fun refresh() {
        val rows = runCatching {
            postgrest.from("app_config").select().decodeList<ConfigRowDto>()
        }.getOrElse { return } // offline or before the migration: keep the last answer
        if (rows.isEmpty()) return
        val values = rows.associate { it.key to it.value?.trim()?.takeIf { v -> v.isNotBlank() } }
        dataStore.edit { prefs ->
            // An untrusted address is never stored; the last good one (or the built-in) stays.
            guard.accept(values["dashboard_url"])?.let { prefs[DASHBOARD_URL] = it }
            values["support_contact"]?.let { prefs[SUPPORT] = it } ?: prefs.remove(SUPPORT)
            values["notice"]?.let { prefs[NOTICE] = it } ?: prefs.remove(NOTICE)
            values["min_version_code"]?.toIntOrNull()?.let { prefs[MIN_VERSION] = it } ?: prefs.remove(MIN_VERSION)
        }
    }

    @Serializable
    private data class ConfigRowDto(
        @SerialName("key") val key: String,
        @SerialName("value") val value: String? = null,
    )

    private companion object {
        val DASHBOARD_URL = stringPreferencesKey("config_dashboard_url")
        val SUPPORT = stringPreferencesKey("config_support_contact")
        val NOTICE = stringPreferencesKey("config_notice")
        val MIN_VERSION = intPreferencesKey("config_min_version_code")
    }
}
