package com.shopmate.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.data.auth.StoreContext
import com.shopmate.domain.model.Licence
import com.shopmate.domain.repository.DeviceRepository
import com.shopmate.domain.repository.LicenceRepository
import com.shopmate.domain.repository.TillInfo
import com.shopmate.domain.repository.SettingsRepository
import com.shopmate.domain.repository.StaffRepository
import com.shopmate.domain.repository.StaffRole
import com.shopmate.domain.repository.StoreChoice
import com.shopmate.domain.repository.StoreRepository
import com.shopmate.domain.repository.TenancyRefusal
import com.shopmate.domain.repository.TenancyResult
import com.shopmate.domain.repository.TillStaff
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Something the owner just did, or why it didn't happen — shown under the action. */
sealed interface TeamNotice {
    data class Refused(val reason: TenancyRefusal) : TeamNotice
    data class StoreAdded(val name: String) : TeamNotice
    data class StaffAdded(val name: String) : TeamNotice
    data class Removed(val name: String) : TeamNotice
    data object PinChanged : TeamNotice
    data class TillRemoved(val name: String) : TeamNotice
}

data class TeamUiState(
    val licence: Licence? = null,
    val staff: List<TillStaff> = emptyList(),
    /** Who unlocked the till; null on a till with no staff yet (the owner's own session). */
    val current: TillStaff? = null,
    val activeStoreName: String = "",
    val notice: TeamNotice? = null,
    val busy: Boolean = false,
    /** The store's tills (§ Devices); empty until read, or offline. */
    val tills: List<TillInfo> = emptyList(),
) {
    /**
     * The till is signed in as the owner, so the owner is at it when they unlocked it — or when
     * nobody can lock it yet. A manager or cashier at the till never manages staff (§ Staff).
     */
    val ownerAtTill: Boolean get() = current?.role == StaffRole.OWNER || (current == null && staff.isEmpty())
    val ownerHasPin: Boolean get() = staff.any { it.role == StaffRole.OWNER }
}

/** Settings › Plan, Add a store and Staff (CLAUDE.md § Tenancy, § Staff, § Licensing). */
@HiltViewModel
class TeamViewModel @Inject constructor(
    private val staffRepository: StaffRepository,
    private val storeRepository: StoreRepository,
    private val licences: LicenceRepository,
    private val storeContext: StoreContext,
    private val deviceRepository: DeviceRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val local = MutableStateFlow(TeamUiState())

    /** The stores as the server names them. The till's own display name can differ. */
    private val serverStores = MutableStateFlow<List<StoreChoice>>(emptyList())

    val uiState: StateFlow<TeamUiState> = combine(
        combine(local, licences.licence, ::Pair),
        staffRepository.staff,
        staffRepository.current,
        combine(storeContext.activeStoreIdFlow, settings.storeName, ::Pair),
        serverStores,
    ) { (state, licence), staff, current, (activeStoreId, localName), stores ->
        state.copy(
            licence = licence,
            staff = staff,
            current = current,
            activeStoreName = stores.firstOrNull { it.id == activeStoreId }?.name ?: localName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamUiState())

    init {
        viewModelScope.launch {
            licences.refresh()
            runCatching { storeRepository.accessibleStores() }.onSuccess { serverStores.value = it }
            storeContext.getActiveStoreId()?.let { staffRepository.pull(it) }
            refreshTills()
        }
    }

    private suspend fun refreshTills() {
        (deviceRepository.tills() as? TenancyResult.Done)?.let { done -> local.update { it.copy(tills = done.value) } }
    }

    /** It stops at its next sync; what it holds stays on it (§ Devices). */
    fun removeTill(till: TillInfo) = act(TeamNotice.TillRemoved(till.name)) {
        deviceRepository.removeTill(till.id).also { refreshTills() }
    }

    fun setOwnerPin(name: String, pin: String) = act(TeamNotice.PinChanged) {
        staffRepository.setOwnerPin(name, pin)
    }

    fun addStaff(name: String, role: StaffRole, pin: String, canManageStock: Boolean) =
        act(TeamNotice.StaffAdded(name.trim())) { staffRepository.add(name, role, pin, canManageStock) }

    fun update(member: TillStaff, role: StaffRole, canManageStock: Boolean) =
        act(null) { staffRepository.update(member.id, role, canManageStock) }

    fun changePin(member: TillStaff, pin: String) = act(TeamNotice.PinChanged) {
        staffRepository.changePin(member.id, pin)
    }

    fun remove(member: TillStaff) = act(TeamNotice.Removed(member.name)) { staffRepository.remove(member.id) }

    fun addStore(name: String) {
        if (name.isBlank()) return
        act(TeamNotice.StoreAdded(name.trim())) {
            val currency = runCatching { storeContext.fetchAccessibleStores() }.getOrDefault(emptyList())
                .firstOrNull { it.id == storeContext.getActiveStoreId() }?.currencySymbol ?: "$"
            when (val result = storeRepository.createStore(name, currency)) {
                is TenancyResult.Done -> TenancyResult.Done(Unit)
                is TenancyResult.Refused -> result
            }
        }
    }

    private fun act(onDone: TeamNotice?, block: suspend () -> TenancyResult<Unit>) {
        viewModelScope.launch {
            local.update { it.copy(busy = true, notice = null) }
            val result = block()
            licences.refresh()
            local.update {
                it.copy(
                    busy = false,
                    notice = when (result) {
                        is TenancyResult.Done -> onDone
                        is TenancyResult.Refused -> TeamNotice.Refused(result.reason)
                    },
                )
            }
        }
    }
}
