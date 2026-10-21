package com.shopmate.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.data.auth.StoreContext
import com.shopmate.domain.repository.SettingsRepository
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
    private val storeContext: StoreContext,
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
            val result = runCatching {
                storeContext.createOrganizationWithStore(orgName.trim(), storeName.trim(), currency)
            }
            result.onSuccess {
                // Mirror the merchant's choices into local settings so the UI is consistent offline.
                settings.setStoreName(storeName.trim())
                settings.setCurrencySymbol(currency)
                _uiState.update { it.copy(isSubmitting = false) }
            }.onFailure {
                _uiState.update { it.copy(isSubmitting = false, error = GENERIC) }
            }
        }
    }

    companion object {
        const val ORG_REQUIRED = "org_required"
        const val STORE_REQUIRED = "store_required"
        const val GENERIC = "generic"
    }
}
