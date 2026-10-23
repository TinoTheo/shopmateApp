package com.shopmate.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.StoreContext
import com.shopmate.domain.model.Licence
import com.shopmate.domain.repository.Invite
import com.shopmate.domain.repository.LicenceRepository
import com.shopmate.domain.repository.SettingsRepository
import com.shopmate.domain.repository.StaffMember
import com.shopmate.domain.repository.StaffRepository
import com.shopmate.domain.repository.StaffRole
import com.shopmate.domain.repository.StoreChoice
import com.shopmate.domain.repository.StoreRepository
import com.shopmate.domain.repository.TenancyRefusal
import com.shopmate.domain.repository.TenancyResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Something the owner just did, or why it didn't happen — shown where they did it. */
sealed interface TeamNotice {
    data class Refused(val reason: TenancyRefusal) : TeamNotice
    data class StoreAdded(val name: String) : TeamNotice
    data class Removed(val email: String) : TeamNotice
}

data class TeamUiState(
    val licence: Licence? = null,
    /** Null for a cashier (who can't see the staff list) and before the list loads. */
    val myRole: StaffRole? = null,
    val myUserId: String? = null,
    val staff: List<StaffMember> = emptyList(),
    /** False until the list has been read once; until then nothing claims who can manage it. */
    val staffLoaded: Boolean = false,
    val activeStoreId: String? = null,
    val activeStoreName: String = "",
    val newInvite: Invite? = null,
    val notice: TeamNotice? = null,
    val busy: Boolean = false,
) {
    val canManageTeam: Boolean get() = myRole == StaffRole.OWNER || myRole == StaffRole.MANAGER
    val isOwner: Boolean get() = myRole == StaffRole.OWNER
}

/**
 * Settings › Plan, Stores and Staff (CLAUDE.md § Tenancy, staff and licensing). Everything here
 * needs the connection; the plan section shows the last licence read, which works offline.
 */
@HiltViewModel
class TeamViewModel @Inject constructor(
    private val staffRepository: StaffRepository,
    private val storeRepository: StoreRepository,
    private val licences: LicenceRepository,
    private val storeContext: StoreContext,
    private val authManager: AuthManager,
    settings: SettingsRepository,
) : ViewModel() {

    private val local = MutableStateFlow(TeamUiState())

    val uiState: StateFlow<TeamUiState> = combine(
        local,
        licences.licence,
        storeContext.activeStoreIdFlow,
        settings.storeName,
    ) { state, licence, activeStoreId, storeName ->
        state.copy(licence = licence, activeStoreId = activeStoreId, activeStoreName = storeName)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            licences.refresh()
            val me = authManager.currentUserId
            when (val result = staffRepository.list()) {
                is TenancyResult.Done -> local.update { state ->
                    state.copy(
                        staff = result.value,
                        staffLoaded = true,
                        myUserId = me,
                        myRole = result.value.firstOrNull { it.userId == me }?.role,
                    )
                }
                is TenancyResult.Refused -> Unit // offline: keep what's shown
            }
        }
    }

    fun invite(role: StaffRole, canManageStock: Boolean) {
        val storeId = uiState.value.activeStoreId ?: return
        launchBusy {
            when (val result = staffRepository.invite(storeId, uiState.value.activeStoreName, role, canManageStock)) {
                is TenancyResult.Done -> local.update { it.copy(newInvite = result.value, notice = null) }
                is TenancyResult.Refused -> local.update { it.copy(notice = TeamNotice.Refused(result.reason)) }
            }
            licences.refresh()
        }
    }

    fun dismissInvite() = local.update { it.copy(newInvite = null) }

    fun changeRole(member: StaffMember, role: StaffRole) = launchBusy {
        staffRepository.changeRole(member.membershipId, role).reportAndRefresh()
    }

    fun setStockRights(member: StaffMember, granted: Boolean) {
        val storeId = uiState.value.activeStoreId ?: return
        launchBusy { staffRepository.setStockRights(member.membershipId, storeId, granted).reportAndRefresh() }
    }

    fun remove(member: StaffMember) = launchBusy {
        when (val result = staffRepository.remove(member.membershipId)) {
            is TenancyResult.Done -> local.update { it.copy(notice = TeamNotice.Removed(member.email)) }
            is TenancyResult.Refused -> local.update { it.copy(notice = TeamNotice.Refused(result.reason)) }
        }
        refresh()
    }

    fun addStore(name: String) {
        if (name.isBlank()) return
        launchBusy {
            val currency = storeContext.fetchAccessibleStores()
                .firstOrNull { it.id == storeContext.getActiveStoreId() }?.currencySymbol ?: "$"
            when (val result = storeRepository.createStore(name, currency)) {
                is TenancyResult.Done<StoreChoice> -> local.update { it.copy(notice = TeamNotice.StoreAdded(result.value.name)) }
                is TenancyResult.Refused -> local.update { it.copy(notice = TeamNotice.Refused(result.reason)) }
            }
            licences.refresh()
        }
    }

    private suspend fun TenancyResult<Unit>.reportAndRefresh() {
        if (this is TenancyResult.Refused) local.update { it.copy(notice = TeamNotice.Refused(reason)) }
        refresh()
    }

    private fun launchBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            local.update { it.copy(busy = true, notice = null) }
            try {
                block()
            } finally {
                local.update { it.copy(busy = false) }
            }
        }
    }
}
