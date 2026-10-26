package com.shopmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.LicenceState
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.StockOrangeSubtle

/**
 * The till's view of the plan, on the POS only once it matters to the cashier: grace reads as a
 * warning (StockOrange tint), expired as stopped (ShopBlack) — § Severity. Never ErrorRed: an
 * ended plan is a fact, not a failure. Nothing shows while the plan is active or merely ending.
 */
@Composable
fun LicenceBanner(state: LicenceState?, modifier: Modifier = Modifier) {
    val (text, stopped) = when (state) {
        is LicenceState.Grace ->
            pluralStringResource(R.plurals.licence_grace, state.daysLeft, state.daysLeft) to false
        LicenceState.Expired -> stringResource(R.string.licence_expired) to true
        else -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (stopped) ShopWhite else ShopBlack,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(if (stopped) ShopBlack else StockOrangeSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}
