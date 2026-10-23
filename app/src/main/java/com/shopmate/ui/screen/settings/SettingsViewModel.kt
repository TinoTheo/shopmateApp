package com.shopmate.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.BuildConfig
import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.StoreContext
import com.shopmate.domain.repository.SettingsRepository
import com.shopmate.domain.repository.StoreChoice
import com.shopmate.domain.repository.StoreRepository
import com.shopmate.domain.repository.StoreSwitch
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
    /** The store the owner asked to move this till to, awaiting their confirmation. */
    val pendingSwitch: StoreOption? = null,
    /** Why the last switch didn't happen, or that it did. Cleared by the next attempt. */
    val switchOutcome: StoreSwitch? = null,
    val isSwitching: Boolean = false,
) {
    val activeStoreName: String? get() = stores.firstOrNull { it.id == activeStoreId }?.name
    val hasMultipleStores: Boolean get() = stores.size > 1
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val authManager: AuthManager,
    private val storeContext: StoreContext,
    private val storeRepository: StoreRepository,
) : ViewModel() {

    private val stores = MutableStateFlow<List<StoreOption>>(emptyList())
    private val userEmail = MutableStateFlow(authManager.currentUserEmail)
    private val switchState = MutableStateFlow(SwitchState())

    private data class SwitchState(
        val pending: StoreOption? = null,
        val outcome: StoreSwitch? = null,
        val busy: Boolean = false,
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(settings.storeName, settings.currencySymbol, ::Pair),
        storeContext.activeStoreIdFlow,
        stores,
        userEmail,
        switchState,
    ) { (storeName, symbol), activeStoreId, storeList, email, switch ->
        SettingsUiState(
            storeName = storeName,
            currencySymbol = symbol,
            userEmail = email,
            stores = storeList,
            activeStoreId = activeStoreId,
            pendingSwitch = switch.pending,
            switchOutcome = switch.outcome,
            isSwitching = switch.busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        refreshStores()
    }

    fun updateStoreName(name: String) { viewModelScope.launch { settings.setStoreName(name) } }
    fun updateCurrencySymbol(symbol: String) { viewModelScope.launch { settings.setCurrencySymbol(symbol) } }
    fun retryFailedSyncs() { viewModelScope.launch { settings.retryFailedSyncEvents() } }
    fun resetAllData() { viewModelScope.launch { settings.resetAllData() } }

    /** Asks first: moving a till clears its local data (§ Tenancy — one store per device). */
    fun requestSwitch(storeId: String) {
        val store = stores.value.firstOrNull { it.id == storeId } ?: return
        switchState.value = SwitchState(pending = store)
    }

    fun cancelSwitch() {
        switchState.value = SwitchState()
    }

    fun confirmSwitch() {
        val store = switchState.value.pending ?: return
        switchState.value = SwitchState(busy = true)
        viewModelScope.launch {
            val outcome = storeRepository.switchTo(StoreChoice(store.id, store.name))
            switchState.value = SwitchState(outcome = outcome)
        }
    }

    /** Sign out of the cloud session; local Room data stays. The root gate returns to auth. */
    fun signOut() {
        viewModelScope.launch {
            storeContext.clearActiveStore()
            authManager.signOut()
        }
    }

    fun refreshStores() {
        viewModelScope.launch {
            userEmail.value = authManager.currentUserEmail
            runCatching { storeRepository.accessibleStores() }
                .getOrNull()
                ?.let { list -> stores.value = list.map { StoreOption(it.id, it.name) } }
        }
    }
}
