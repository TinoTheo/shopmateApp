package com.shopmate.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shopmate.data.local.AppDatabase
import com.shopmate.data.local.dao.SyncEventDao
import com.shopmate.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val syncEventDao: SyncEventDao,
    private val db: AppDatabase,
) : SettingsRepository {

    override val storeName: Flow<String> =
        dataStore.data.map { it[STORE_NAME_KEY] ?: "" }

    override val currencySymbol: Flow<String> =
        dataStore.data.map { it[CURRENCY_SYMBOL_KEY] ?: DEFAULT_SYMBOL }

    override val targetMargin: Flow<Double> =
        dataStore.data.map { it[TARGET_MARGIN_KEY] ?: DEFAULT_TARGET_MARGIN }

    override suspend fun setStoreName(name: String) {
        dataStore.edit { it[STORE_NAME_KEY] = name }
    }

    override suspend fun setCurrencySymbol(symbol: String) {
        dataStore.edit { it[CURRENCY_SYMBOL_KEY] = symbol }
    }

    override suspend fun setTargetMargin(margin: Double) {
        dataStore.edit { it[TARGET_MARGIN_KEY] = margin.coerceIn(0.0, MAX_TARGET_MARGIN) }
    }

    override suspend fun retryFailedSyncEvents() = syncEventDao.retryFailed()

    override suspend fun resetAllData() {
        // clearAllTables() runs its own transaction — must NOT be nested in withTransaction.
        db.clearAllTables()
        dataStore.edit { it.clear() }
    }

    companion object {
        const val DEFAULT_SYMBOL = "$"
        val STORE_NAME_KEY = stringPreferencesKey("store_name")
        val CURRENCY_SYMBOL_KEY = stringPreferencesKey("currency_symbol")

        /** CLAUDE.md § Settings resolves the default at 30%. */
        const val DEFAULT_TARGET_MARGIN = 0.30
        const val MAX_TARGET_MARGIN = 0.95
        val TARGET_MARGIN_KEY = doublePreferencesKey("target_margin")
    }
}
