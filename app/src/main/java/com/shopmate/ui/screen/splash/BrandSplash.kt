package com.shopmate.ui.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.shopmate.BuildConfig
import com.shopmate.R
import com.shopmate.ui.theme.Inter
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.DeviceLayout

/**
 * Brand splash. The system splash already shows the lockup — wordmark with "uncompromising
 * control" — at [SystemSplashLockupWidth]; this picks it up at that size and place, so the
 * handover can't be seen, then lets it grow to its full size. **No spinner, no progress bar.**
 * The root holds it for a set time, then crossfades into the app.
 *
 * The tagline is part of the brand owner's lockup art (ic_shopmate_lockup), never set as text.
 */
@Composable
fun BrandSplash(deviceLayout: DeviceLayout, storeName: String) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(TradeGreen),
    ) {
        val fullWidth = when (deviceLayout) {
            DeviceLayout.COMPACT -> min(maxWidth * 0.72f, 300.dp)
            DeviceLayout.EXPANDED -> min(maxWidth * 0.5f, 460.dp)
        }
        // From the system splash's size to full size: the logo appears to settle, not to jump.
        val growth = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            growth.animateTo(1f, tween(durationMillis = GROW_MS, easing = FastOutSlowInEasing))
        }
        Image(
            painter = painterResource(R.drawable.ic_shopmate_lockup),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .align(Alignment.Center)
                .width(lerp(SystemSplashLockupWidth, fullWidth, growth.value))
                // Height follows the width. Left to the drawable's own 280dp size, the image
                // capped its width there however wide it was asked to be.
                .aspectRatio(LOCKUP_ASPECT),
        )
        if (deviceLayout == DeviceLayout.EXPANDED) {
            // 1i: store and version at the foot of the tablet splash.
            Text(
                text = listOfNotNull(storeName.ifBlank { null }, "v${BuildConfig.VERSION_NAME}")
                    .joinToString(" · "),
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    fontFeatureSettings = "tnum",
                    color = ShopWhite.copy(alpha = 0.6f),
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 36.dp),
            )
        }
    }
}

/**
 * How wide the system splash draws the lockup (`ic_splash_lockup`: 64% of the splash icon). Measured
 * on Android 15; other versions draw the splash icon a little larger or smaller, and the growth
 * hides the difference.
 */
private val SystemSplashLockupWidth = 170.dp

private const val GROW_MS = 600

/** The lockup artwork's proportions (7055 × 2002). */
private const val LOCKUP_ASPECT = 7055f / 2002f
