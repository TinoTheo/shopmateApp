package com.shopmate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopMateTheme
import com.shopmate.ui.theme.StockOrange
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.theme.WarmSand

/**
 * Margin measured in **money** (CLAUDE.md § Gauges).
 *
 * One continuous track. Cost fills from the left in [TradeGreen]; the store's target margin
 * sits as a 2dp [ShopBlack] notch; cost past the notch renders the overflow in [StockOrange].
 * Selling at or below cost fills the whole track [ShopBlack] — that is the Stopped tier, and
 * it needs no red (§ Severity).
 *
 * Money is continuous, so the track is continuous — the deliberate contrast with the segmented
 * [RunwayGauge]. Both are 52×16dp and sit in the same slot.
 *
 * @param margin the product's actual margin as a fraction of the selling price.
 * @param targetMargin the store setting, from `SettingsRepository` — never a constant.
 */
@Composable
fun MarginGauge(
    margin: Double,
    targetMargin: Double,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(width = GAUGE_WIDTH, height = GAUGE_HEIGHT)) {
        Canvas(Modifier.size(width = GAUGE_WIDTH, height = GAUGE_HEIGHT)) {
            val radius = CornerRadius(TRACK_RADIUS.toPx())
            drawRoundRect(TRACK_COLOUR, size = size, cornerRadius = radius)

            if (margin <= 0.0) {
                // Selling at or below cost: the whole track goes black. Nothing else to read.
                drawRoundRect(ShopBlack, size = size, cornerRadius = radius)
                return@Canvas
            }

            // Cost as a share of the selling price is the inverse of margin.
            val costShare = (1.0 - margin).coerceIn(0.0, 1.0).toFloat()
            val notchShare = (1.0 - targetMargin).coerceIn(0.0, 1.0).toFloat()
            val costWidth = size.width * costShare
            val notchX = size.width * notchShare

            drawRoundRect(
                color = TradeGreen,
                size = Size(costWidth.coerceAtMost(notchX), size.height),
                cornerRadius = radius,
            )
            // Cost past the notch is the part that eats the target — Warning tier.
            if (costWidth > notchX) {
                drawRect(
                    color = StockOrange,
                    topLeft = Offset(notchX, 0f),
                    size = Size(costWidth - notchX, size.height),
                )
            }
            drawRect(
                color = ShopBlack,
                topLeft = Offset(notchX - NOTCH_WIDTH.toPx() / 2f, 0f),
                size = Size(NOTCH_WIDTH.toPx(), size.height),
            )
        }
    }
}

private val TRACK_RADIUS = 3.dp
private val NOTCH_WIDTH = 2.dp
private val TRACK_COLOUR: Color = WarmSand

@Preview(showBackground = true, backgroundColor = 0xFFFDFCFA)
@Composable
private fun MarginGaugePreview() {
    ShopMateTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            MarginGauge(margin = 0.42, targetMargin = 0.30)   // comfortably above target
            MarginGauge(margin = 0.30, targetMargin = 0.30)   // exactly on the notch
            MarginGauge(margin = 0.14, targetMargin = 0.30)   // cost has eaten past the notch
            MarginGauge(margin = -0.05, targetMargin = 0.30)  // selling at a loss
        }
    }
}
