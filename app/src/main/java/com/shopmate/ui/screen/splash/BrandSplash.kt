package com.shopmate.ui.screen.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.BuildConfig
import com.shopmate.ui.components.ShopMateMark
import com.shopmate.ui.components.ShopMateWordmark
import com.shopmate.ui.theme.Inter
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.theme.TradeGreenDeep
import com.shopmate.ui.util.DeviceLayout

/**
 * Brand splash, stage 2 (brand handoff 1h phone / 1i tablet). Shown while the app is still
 * opening after the system splash hands over — local work that takes under a second — so it has
 * **no spinner and no progress bar**. The root crossfades it into the app.
 *
 * The tagline sits under the wordmark in the brand art, in a serif. It ships with the lockup
 * vector from the brand owner; until then the wordmark stands alone (see ShopMateWordmark).
 */
@Composable
fun BrandSplash(deviceLayout: DeviceLayout, storeName: String) {
    when (deviceLayout) {
        DeviceLayout.COMPACT -> PhoneSplash()
        DeviceLayout.EXPANDED -> TabletSplash(storeName)
    }
}

/** 1h: an oversized M in Trade Green Deep bleeding off the top-left, lockup bottom-start. */
@Composable
private fun PhoneSplash() {
    Box(
        Modifier
            .fillMaxSize()
            .background(TradeGreen)
            .clipToBounds(),
    ) {
        ShopMateMark(
            color = TradeGreenDeep,
            modifier = Modifier
                .offset(x = (-120).dp, y = (-40).dp)
                .requiredSize(width = 560.dp, height = 748.dp),
        )
        ShopMateWordmark(
            width = 240.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 32.dp, bottom = 72.dp),
        )
    }
}

/** 1i: the lockup centred, store and version at the foot. */
@Composable
private fun TabletSplash(storeName: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(TradeGreen),
    ) {
        ShopMateWordmark(width = 460.dp, modifier = Modifier.align(Alignment.Center))
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
