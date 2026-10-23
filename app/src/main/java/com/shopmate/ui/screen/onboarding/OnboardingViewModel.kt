package com.shopmate.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.domain.repository.SettingsRepository
import com.shopmate.domain.repository.StoreRepository
import com.shopmate.domain.repository.StoreSwitch
import com.shopmate.domain.repository.TenancyRefusal
import com.shopmate.domain.repository.TenancyResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun clearError() = _uiState.update { it.copy(error = null) }

    /**
     * Creates the org + store server-side and marks it active. On success [StoreContext] sets the
     * active store id, which flips the root gate to the main app — no explicit navigation needed.
     */
    fun createShop(orgName: String, storeName: String, currency: String) {
        when {
            orgName.isBlank() -> { _uiState.update { it.copy(error = ORG_REQUIRED) }; return }
            storeName.isBlank() -> { _uiState.update { it.copy(error = STORE_REQUIRED) }; return }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            when (val result = storeRepository.createOrganization(orgName, storeName, currency)) {
                is TenancyResult.Done -> {
                    // Mirror the merchant's choices into local settings so the UI is consistent offline.
                    settings.setStoreName(storeName.trim())
                    settings.setCurrencySymbol(currency)
                    _uiState.update { it.copy(isSubmitting = false, error = result.value.errorMarker()) }
                }
                is TenancyResult.Refused -> _uiState.update { it.copy(isSubmitting = false, error = GENERIC) }
            }
        }
    }

    /** Join an existing shop with the code its owner or manager shared. */
    fun joinShop(code: String) {
        if (code.isBlank()) { _uiState.update { it.copy(error = CODE_REQUIRED) }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            val error = when (val result = storeRepository.joinWithCode(code)) {
                is TenancyResult.Done -> result.value.errorMarker()
                is TenancyResult.Refused -> when (result.reason) {
                    TenancyRefusal.INVITE_NOT_FOUND -> CODE_NOT_FOUND
                    TenancyRefusal.OFFLINE -> OFFLINE
                    else -> GENERIC
                }
            }
            _uiState.update { it.copy(isSubmitting = false, error = error) }
        }
    }

    /** The till couldn't move to the new store yet — something from another store is unsent. */
    private fun StoreSwitch.errorMarker(): String? = when (this) {
        StoreSwitch.Switched -> null
        StoreSwitch.Offline -> OFFLINE
        is StoreSwitch.NotSentYet -> NOT_SENT
    }

    companion object {
        const val ORG_REQUIRED = "org_required"
        const val STORE_REQUIRED = "store_required"
        const val GENERIC = "generic"
        const val CODE_REQUIRED = "code_required"
        const val CODE_NOT_FOUND = "code_not_found"
        const val OFFLINE = "offline"
        const val NOT_SENT = "not_sent"
    }
}
