package com.shopmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.CheckoutNeon
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.NumberSmall
import com.shopmate.ui.theme.StockGreen
import com.shopmate.ui.theme.StockOrange
import com.shopmate.ui.theme.StockRed
import com.shopmate.ui.theme.WarmSand
import com.shopmate.ui.theme.WarmWhite

/**
 * Inline stock indicator. Green/orange for healthy/low (coloured dot + count), red "OUT" pill
 * when empty or negative (oversold). Never uses Checkout Neon — it is too bright for inline text.
 */
@Composable
fun StockStatusBadge(
    quantity: Int,
    threshold: Int,
    unit: String,
    modifier: Modifier = Modifier,
) {
    if (quantity <= 0) {
        Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = StockRed) {
            Text(
                text = "Sold out",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = WarmWhite,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        return
    }
    val color = if (quantity <= threshold) StockOrange else StockGreen
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        // Tabular figures so counts line up down a list.
        Text(
            text = "  $quantity $unit",
            style = NumberSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/**
 * Sync queue pill. Neon (success) when nothing is pending, beige when items are queued,
 * red when the last sync errored.
 */
@Composable
fun SyncStatusBadge(
    pendingCount: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val (bg, fg, label) = when {
        isError -> Triple(ErrorRed, WarmWhite, "Couldn't sync")
        pendingCount == 0 -> Triple(CheckoutNeon, Ink, "Up to date")
        pendingCount == 1 -> Triple(WarmSand, Ink, "1 sale waiting")
        else -> Triple(WarmSand, Ink, "$pendingCount sales waiting")
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = bg) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}
