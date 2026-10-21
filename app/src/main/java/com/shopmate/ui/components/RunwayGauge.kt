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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shopmate.domain.model.ProductInsight
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopMateTheme
import com.shopmate.ui.theme.StockOrange
import com.shopmate.ui.theme.TradeGreen
import kotlin.math.floor

/**
 * Stock runway measured in **time** (CLAUDE.md § Gauges).
 *
 * Seven discrete segments, one per day ahead: filled [TradeGreen] while stock lasts, the
 * partial day in [StockOrange], hollow after it runs out. Days are discrete, so the gauge is
 * segmented — this is the Stock half of the shared instrument, and [MarginGauge] is the other.
 *
 * When [confident] is false **every segment renders hollow**. The gauge must be incapable of
 * projecting from thin data (§ Velocity confidence rule).
 *
 * @param daysOfCover how many days the current stock lasts. Null reads as no cover at all.
 */
@Composable
fun RunwayGauge(
    daysOfCover: Double?,
    confident: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(width = GAUGE_WIDTH, height = GAUGE_HEIGHT)) {
        Canvas(Modifier.size(width = GAUGE_WIDTH, height = GAUGE_HEIGHT)) {
            val gapPx = SEGMENT_GAP.toPx()
            val radius = CornerRadius(SEGMENT_RADIUS.toPx())
            val segmentWidth = (size.width - gapPx * (SEGMENTS - 1)) / SEGMENTS
            val outline = Stroke(width = HOLLOW_STROKE.toPx())

            val cover = if (confident) daysOfCover ?: 0.0 else 0.0
            val wholeDays = floor(cover).toInt()
            val hasPartialDay = confident && cover > wholeDays

            repeat(SEGMENTS) { index ->
                val left = index * (segmentWidth + gapPx)
                val topLeft = Offset(left, 0f)
                val segmentSize = Size(segmentWidth, size.height)
                when {
                    // A hollow outline is also what an unconfident gauge draws, every segment.
                    !confident || index >= wholeDays + (if (hasPartialDay) 1 else 0) ->
                        drawRoundRect(ReceiptBeige, topLeft, segmentSize, radius, outline)

                    index < wholeDays ->
                        drawRoundRect(TradeGreen, topLeft, segmentSize, radius)

                    else ->
                        drawRoundRect(StockOrange, topLeft, segmentSize, radius)
                }
            }
        }
    }
}

/** Shared geometry: both gauges sit in the same slot at the same size so they read the same way. */
val GAUGE_WIDTH = 52.dp
val GAUGE_HEIGHT = 16.dp

private const val SEGMENTS = ProductInsight.RUNWAY_DAYS
private val SEGMENT_GAP = 2.dp
private val SEGMENT_RADIUS = 1.5.dp
private val HOLLOW_STROKE = 1.dp

@Preview(showBackground = true, backgroundColor = 0xFFFDFCFA)
@Composable
private fun RunwayGaugePreview() {
    ShopMateTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            RunwayGauge(daysOfCover = 9.0, confident = true)
            RunwayGauge(daysOfCover = 3.4, confident = true)
            RunwayGauge(daysOfCover = 0.0, confident = true)
            RunwayGauge(daysOfCover = 5.0, confident = false)
        }
    }
}
