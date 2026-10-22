package com.shopmate.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen

/**
 * Screens shown before the app frame — sign-in and onboarding (copy-pass handoff 3h, 3i). A
 * Trade Green band carries the stacked logo, straight on the green, and a white sheet with the
 * frame's 24dp top-start corner holds the form.
 *
 * The band is why the app-wide white status-bar icons are right here too: the status bar always
 * sits on green, so no screen switches icon colour.
 *
 * @param bandHeight the band below the status bar (the handoff's 210dp / 170dp less the 44dp
 *   status bar it includes).
 */
@Composable
fun BrandBand(
    bandHeight: Dp,
    modifier: Modifier = Modifier,
    sheet: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(TradeGreen),
    ) {
        Box(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(bandHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_shopmate_stacked),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(start = 28.dp)
                    .width(132.dp),
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp))
                .background(ShopWhite)
                .padding(top = 32.dp, start = 24.dp, end = 24.dp),
            content = sheet,
        )
    }
}

/** Band heights from the handoff, less the 44dp status bar each includes. */
val SignInBandHeight = 166.dp
val OnboardingBandHeight = 126.dp
