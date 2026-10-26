package com.shopmate.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.shopmate.domain.repository.DeviceRole
import com.shopmate.ui.navigation.AppNavGraph
import com.shopmate.ui.screen.device.DeviceRoleScreen
import com.shopmate.ui.screen.device.RemoteHomeScreen
import com.shopmate.ui.screen.device.TillRemovedScreen
import com.shopmate.ui.screen.auth.AuthScreen
import com.shopmate.ui.screen.lock.PinLockScreen
import com.shopmate.ui.screen.onboarding.OnboardingScreen
import com.shopmate.ui.screen.splash.BrandSplash
import com.shopmate.ui.util.DeviceLayout
import kotlinx.coroutines.delay

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
    val deviceRole by viewModel.deviceRole.collectAsState()
    val tillRemoved by viewModel.tillRemoved.collectAsState()

    // The lockup stays up for a set time so it can be read: a start that resolves in 50ms would
    // otherwise flash it. Saveable, so rotating the till doesn't show it again.
    var splashShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SPLASH_MINIMUM_MS)
        splashShown = true
    }

    // 200ms crossfade: BrandSplash into whichever screen the session resolves to.
    AnimatedContent(
        targetState = if (splashShown) session else SessionState.LOADING,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label = "sessionGate",
    ) { state ->
        when (state) {
            SessionState.LOADING -> BrandSplash(deviceLayout = deviceLayout, storeName = storeName)
            SessionState.SIGNED_OUT -> AuthScreen()
            SessionState.NEEDS_ONBOARDING -> OnboardingScreen()
            // Till or remote first (§ Devices); a till with staff then opens on "Who's selling?".
            SessionState.READY -> when (deviceRole) {
                null -> BrandSplash(deviceLayout = deviceLayout, storeName = storeName)
                DeviceRole.UNDECIDED -> DeviceRoleScreen(storeName = storeName)
                DeviceRole.REMOTE -> RemoteHomeScreen(storeName = storeName)
                DeviceRole.TILL -> when {
                    tillRemoved -> TillRemovedScreen(storeName = storeName)
                    locked -> PinLockScreen()
                    else -> AppNavGraph(deviceLayout = deviceLayout)
                }
            }
        }
    }
}

/** Long enough to read the tagline, as WhatsApp holds its own start screen (~2s). */
private const val SPLASH_MINIMUM_MS = 2_000L
