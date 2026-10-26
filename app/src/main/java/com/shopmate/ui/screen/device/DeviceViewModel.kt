package com.shopmate.ui.screen.device

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.BuildConfig
import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.StoreContext
import com.shopmate.domain.repository.DeviceRepository
import com.shopmate.domain.repository.StaffRepository
import com.shopmate.domain.repository.StoreToday
import com.shopmate.domain.repository.TenancyRefusal
import com.shopmate.domain.repository.TenancyResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceUiState(
    /** Preselects "Yes, sell here" on a device that has sold before. */
    val hasSold: Boolean = false,
    val suggestedName: String = Build.MODEL.orEmpty(),
    val busy: Boolean = false,
    val refusal: TenancyRefusal? = null,
    val today: StoreToday? = null,
    val todayRefusal: TenancyRefusal? = null,
    val webDashboardUrl: String = BuildConfig.WEB_DASHBOARD_URL,
)

/** The one-time till-or-remote question, the remote home, and a removed till (§ Devices). */
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val authManager: AuthManager,
    private val storeContext: StoreContext,
    private val staffRepository: StaffRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceUiState())
    val state: StateFlow<DeviceUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.update { it.copy(hasSold = deviceRepository.hasSold()) } }
    }

    fun becomeTill(name: String) = act { deviceRepository.becomeTill(name) }

    fun becomeRemote() = act { deviceRepository.becomeRemote() }

    fun sellHereInstead() {
        viewModelScope.launch { deviceRepository.askAgain() }
    }

    fun refreshToday() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (val result = deviceRepository.storeToday()) {
                is TenancyResult.Done -> _state.update { it.copy(busy = false, today = result.value, todayRefusal = null) }
                is TenancyResult.Refused -> _state.update { it.copy(busy = false, todayRefusal = result.reason) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            storeContext.clearActiveStore()
            staffRepository.lock()
            authManager.signOut()
        }
    }

    private fun act(block: suspend () -> TenancyResult<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, refusal = null) }
            val result = block()
            _state.update {
                it.copy(busy = false, refusal = (result as? TenancyResult.Refused)?.reason)
            }
        }
    }
}
