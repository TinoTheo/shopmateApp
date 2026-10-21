package com.shopmate.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.shopmate.ui.navigation.AppNavGraph
import com.shopmate.ui.screen.auth.AuthScreen
import com.shopmate.ui.screen.onboarding.OnboardingScreen
import com.shopmate.ui.theme.TradeGreen
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

    AnimatedContent(
        targetState = session,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "sessionGate",
    ) { state ->
        when (state) {
            SessionState.LOADING -> LoadingGate()
            SessionState.SIGNED_OUT -> AuthScreen()
            SessionState.NEEDS_ONBOARDING -> OnboardingScreen()
            SessionState.READY -> AppNavGraph(deviceLayout = deviceLayout)
        }
    }
}

@Composable
private fun LoadingGate() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TradeGreen)
    }
}
