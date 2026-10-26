package com.shopmate.ui.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.shopmate.BuildConfig
import com.shopmate.R
import com.shopmate.ui.theme.Inter
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.DeviceLayout

/**
 * Brand splash. The system splash is plain Trade Green (no icon), so this is the first thing the
 * app shows: the lockup — wordmark with "uncompromising control" — centred, on phone and tablet.
 * Local work that takes under a second, so **no spinner and no progress bar**; the root keeps it
 * up for a short minimum so it never flickers, then crossfades into the app.
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
        val lockupWidth = when (deviceLayout) {
            DeviceLayout.COMPACT -> min(maxWidth * 0.72f, 300.dp)
            DeviceLayout.EXPANDED -> min(maxWidth * 0.5f, 460.dp)
        }
        Image(
            painter = painterResource(R.drawable.ic_shopmate_lockup),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .align(Alignment.Center)
                .width(lockupWidth),
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
