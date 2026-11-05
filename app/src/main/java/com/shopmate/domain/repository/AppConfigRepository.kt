package com.shopmate.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Settings ShopMate changes from the Supabase dashboard without a new APK (§ Remote settings).
 * Every value is optional and has a safe default; nothing here can stop a shop working.
 */
data class AppConfig(
    /** The web dashboard, on a trusted domain only; null when there is none yet. */
    val dashboardUrl: String?,
    /** "Need help?" in Settings, e.g. a WhatsApp number. */
    val supportContact: String?,
    /** True when ShopMate has asked for a newer version than this one. A notice, never a lock. */
    val updateAvailable: Boolean,
    /** One line from ShopMate, e.g. planned maintenance. */
    val notice: String?,
)

interface AppConfigRepository {
    /** The last settings read, kept for offline use; the APK's built-in values until then. */
    val config: Flow<AppConfig>

    /** Reads the settings again. Offline or refused, it keeps what it had. */
    suspend fun refresh()
}
