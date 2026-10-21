package com.shopmate.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.BuildConfig
import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.StoreContext
import com.shopmate.data.remote.ShopMateApi
import com.shopmate.data.remote.dto.StoreDto
import com.shopmate.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoreOption(val id: String, val name: String)

data class SettingsUiState(
    val storeName: String = "",
    val currencySymbol: String = "$",
    // Cloud account (Supabase session)
    val userEmail: String? = null,
    val stores: List<StoreOption> = emptyList(),
    val activeStoreId: String? = null,
    val appVersion: String = BuildConfig.VERSION_NAME,
) {
    val activeStoreName: String? get() = stores.firstOrNull { it.id == activeStoreId }?.name
    val hasMultipleStores: Boolean get() = stores.size > 1
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val authManager: AuthManager,
    private val storeContext: StoreContext,
    // Kept for the legacy dashboard connection test; no longer surfaced in the UI.
    private val api: ShopMateApi,
) : ViewModel() {

    private val stores = MutableStateFlow<List<StoreOption>>(emptyList())
    private val userEmail = MutableStateFlow(authManager.currentUserEmail)

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.storeName,
        settings.currencySymbol,
        storeContext.activeStoreIdFlow,
        stores,
        userEmail,
    ) { storeName, symbol, activeStoreId, storeList, email ->
        SettingsUiState(
            storeName = storeName,
            currencySymbol = symbol,
            userEmail = email,
            stores = storeList,
            activeStoreId = activeStoreId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        refreshStores()
    }

    fun updateStoreName(name: String) { viewModelScope.launch { settings.setStoreName(name) } }
    fun updateCurrencySymbol(symbol: String) { viewModelScope.launch { settings.setCurrencySymbol(symbol) } }
    fun retryFailedSyncs() { viewModelScope.launch { settings.retryFailedSyncEvents() } }
    fun resetAllData() { viewModelScope.launch { settings.resetAllData() } }

    fun switchStore(storeId: String) {
        viewModelScope.launch { storeContext.setActiveStoreId(storeId) }
    }

    /** Sign out of the cloud session; local Room data stays. The root gate returns to auth. */
    fun signOut() {
        viewModelScope.launch {
            storeContext.clearActiveStore()
            authManager.signOut()
        }
    }

    private fun refreshStores() {
        viewModelScope.launch {
            userEmail.value = authManager.currentUserEmail
            runCatching { storeContext.fetchAccessibleStores() }
                .getOrNull()
                ?.let { list -> stores.value = list.map { StoreOption(it.id, it.name) } }
        }
    }
}
