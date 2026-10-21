package com.shopmate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.StoreContext
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
) : ViewModel() {

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
     * On sign-in with no active store yet, adopt the first accessible store (RLS-scoped) before
     * deciding between onboarding and the main app. A brand-new owner has none → onboarding.
     */
    private fun resolveStoreThenObserve(): Flow<SessionState> = flow {
        emit(SessionState.LOADING)
        if (storeContext.getActiveStoreId() == null) {
            runCatching { storeContext.fetchAccessibleStores() }
                .getOrDefault(emptyList())
                .firstOrNull()
                ?.let { storeContext.setActiveStoreId(it.id) }
        }
        emitAll(
            storeContext.activeStoreIdFlow.map { activeId ->
                if (activeId != null) SessionState.READY else SessionState.NEEDS_ONBOARDING
            },
        )
    }
}
