package com.shopmate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.StoreContext
import com.shopmate.domain.repository.SettingsRepository
import com.shopmate.domain.repository.DeviceRepository
import com.shopmate.domain.repository.DeviceRole
import com.shopmate.domain.repository.StoreRepository
import com.shopmate.domain.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Top-level app gate. Drives which of auth / onboarding / main app the root shows. */
enum class SessionState { LOADING, SIGNED_OUT, NEEDS_ONBOARDING, READY }

@HiltViewModel
class RootViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val storeContext: StoreContext,
    private val storeRepository: StoreRepository,
    staffRepository: StaffRepository,
    deviceRepository: DeviceRepository,
    settings: SettingsRepository,
) : ViewModel() {

    /** Till or remote, asked once per store on each device (§ Devices). */
    val deviceRole: StateFlow<DeviceRole?> = deviceRepository.role
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The owner removed this till: it shows so, and stops syncing. */
    val tillRemoved: StateFlow<Boolean> = deviceRepository.removed
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** The store has staff and nobody has unlocked the till yet: show "Who's selling?". */
    val locked: StateFlow<Boolean> = staffRepository.locked
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** For the tablet splash footer. Empty until a store has been named. */
    val storeName: StateFlow<String> = settings.storeName
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionState: StateFlow<SessionState> =
        authManager.isSignedIn
            .flatMapLatest { signedIn ->
                if (!signedIn) {
                    flowOf(SessionState.SIGNED_OUT)
                } else {
                    resolveStoreThenObserve()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.LOADING)

    /**
     * On sign-in with no active store yet, go back to the store this till's data belongs to, or
     * the member's first store (RLS-scoped), before deciding between onboarding and the main app.
     * A brand-new owner has none → onboarding. So does a member whose stores don't include this
     * till's while it still holds unsent sales: those are never moved to another store.
     */
    private fun resolveStoreThenObserve(): Flow<SessionState> = flow {
        emit(SessionState.LOADING)
        if (storeContext.getActiveStoreId() == null) {
            storeRepository.resolveOnSignIn()
        }
        emitAll(
            storeContext.activeStoreIdFlow.map { activeId ->
                if (activeId != null) SessionState.READY else SessionState.NEEDS_ONBOARDING
            },
        )
    }
}
