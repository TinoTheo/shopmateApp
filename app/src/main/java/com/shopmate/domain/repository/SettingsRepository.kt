package com.shopmate.domain.repository

import kotlinx.coroutines.flow.Flow

/** Merchant-configurable settings, backed by DataStore + the auth-token store. */
interface SettingsRepository {
    val baseUrl: Flow<String>
    val storeName: Flow<String>
    val currencySymbol: Flow<String>

    /**
     * Store target margin as a fraction (0.30 = 30%). Drives the `MarginGauge` notch and the
     * "holds your target" price suggestion. A store setting, never a constant
     * (CLAUDE.md § Settings). Default 30%.
     */
    val targetMargin: Flow<Double>
    val authToken: Flow<String?>

    suspend fun setBaseUrl(url: String)
    suspend fun setStoreName(name: String)
    suspend fun setCurrencySymbol(symbol: String)

    /** [margin] is a fraction; values outside 0..0.95 are clamped. */
    suspend fun setTargetMargin(margin: Double)
    suspend fun setAuthToken(token: String)
    suspend fun clearAuthToken()

    /** Re-queues failed sync events so the worker retries them (does not delete — CLAUDE.md rule 7). */
    suspend fun retryFailedSyncEvents()

    /** Nuclear reset: wipes every Room table and all stored preferences. */
    suspend fun resetAllData()
}
