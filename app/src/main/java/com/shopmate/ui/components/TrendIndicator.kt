package com.shopmate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.WarmWhite
import com.shopmate.ui.theme.TrendDown
import com.shopmate.ui.theme.TrendUp
import kotlin.math.abs

/**
 * Trend arrow + percentage + label. Used ONLY inside the Revenue Hero Card (white-text context),
 * which is one of the three permitted uses of Checkout Neon (the trend-up arrow).
 * Positive → neon up, negative → soft-red down, flat (±1%) → faded white dash.
 */
@Composable
fun TrendIndicator(
    percentageChange: Float,
    label: String,
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val (icon, tint, fallback) = when {
        percentageChange > 1f ->
            Triple(Icons.Filled.ArrowUpward, TrendUp, "+%.1f%% %s".format(percentageChange, label))
        percentageChange < -1f ->
            Triple(Icons.Filled.ArrowDownward, TrendDown, "-%.1f%% %s".format(abs(percentageChange), label))
        else ->
            Triple(Icons.Filled.Remove, WarmWhite.copy(alpha = 0.6f), "Same $label")
    }
    // [text] lets the caller speak the trend in plain words ("Up 12% — better than yesterday")
    // while the arrow and colour still come from the raw number.
    val display = text ?: fallback
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text = display, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}
