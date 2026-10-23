package com.shopmate.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.data.auth.AuthManager
import com.shopmate.domain.repository.SettingsRepository
import com.shopmate.domain.repository.StaffRepository
import com.shopmate.domain.repository.TillStaff
import com.shopmate.domain.repository.SyncRepository
import com.shopmate.sync.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Provides the merchant's store name and sync status to the app bar, reactively. */
@HiltViewModel
class AppBarViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    syncRepository: SyncRepository,
    networkMonitor: NetworkMonitor,
    authManager: AuthManager,
    private val staffRepository: StaffRepository,
) : ViewModel() {
    /** Who unlocked the till; null on a till with no staff. */
    val person: StateFlow<TillStaff?> = staffRepository.current

    /** "Switch cashier": back to "Who's selling?". */
    fun lock() = staffRepository.lock()

    val storeName: StateFlow<String> = settingsRepository.storeName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Unsynced event count for the header's sync dot. */
    val pendingSyncCount: StateFlow<Int> = syncRepository.getPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Uploads the server refused — shown before anything else, so "Up to date" is never a lie. */
    val failedSyncCount: StateFlow<Int> = syncRepository.getFailedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Offline prefixes the status ("Offline · 3 waiting"); it is a state, never an error. */
    val online: StateFlow<Boolean> = networkMonitor.isOnline

    /** Signed out on this till: uploads wait for sign-in ("Paused"). */
    val paused: StateFlow<Boolean> = authManager.needsReauth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
