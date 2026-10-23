package com.shopmate.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.shopmate.ui.navigation.AppNavGraph
import com.shopmate.ui.screen.lock.PinLockScreen
import com.shopmate.ui.screen.auth.AuthScreen
import com.shopmate.ui.screen.onboarding.OnboardingScreen
import com.shopmate.ui.screen.splash.BrandSplash
import com.shopmate.ui.util.DeviceLayout

/**
 * Auth gate over the whole app: signed out → [AuthScreen]; signed in without a store →
 * [OnboardingScreen]; signed in with an active store → the main [AppNavGraph]. Sign-out and
 * onboarding completion both flip [RootViewModel.sessionState], so navigation is reactive —
 * no imperative routing between these top-level states.
 */
@Composable
fun ShopMateRoot(
    deviceLayout: DeviceLayout,
    viewModel: RootViewModel = hiltViewModel(),
) {
    val session by viewModel.sessionState.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val locked by viewModel.locked.collectAsState()

    // 200ms crossfade: BrandSplash into whichever screen the session resolves to.
    AnimatedContent(
        targetState = session,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label = "sessionGate",
    ) { state ->
        when (state) {
            SessionState.LOADING -> BrandSplash(deviceLayout = deviceLayout, storeName = storeName)
            SessionState.SIGNED_OUT -> AuthScreen()
            SessionState.NEEDS_ONBOARDING -> OnboardingScreen()
            // A till with staff opens on "Who's selling?" until someone enters their PIN.
            SessionState.READY -> if (locked) PinLockScreen() else AppNavGraph(deviceLayout = deviceLayout)
        }
    }
}
