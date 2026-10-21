package com.shopmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.NumberRowSub
import com.shopmate.ui.theme.NumberRowValue
import com.shopmate.ui.theme.ShopBlackMid
import com.shopmate.ui.theme.ShopMateTheme
import com.shopmate.ui.theme.StockOrangeSubtle

/**
 * Severity tiers, mapped onto colours already in the palette (CLAUDE.md § Severity).
 * No tier introduces a new colour, and none of them is red — `ErrorRed` is for failures.
 */
enum class Severity {
    /** Nothing needed. Default treatment. */
    Healthy,

    /** Will hurt soon: runs out this week, thin margin, cost rose. Tints the row surface. */
    Warning,

    /** Already costing money: out of stock, selling at a loss. Carries a [StoppedChip]. */
    Stopped,
}

/**
 * **The** row shared by Stock and Products. Building it once is what makes the two screens read
 * as siblings — do not fork it per screen (CLAUDE.md § Component library).
 *
 * Slots, in order: name over meta · gauge · trailing value over sub. The gauge slot takes
 * [RunwayGauge] on Stock and [MarginGauge] on Products — same slot, same size, one shape the
 * cashier learns to read.
 *
 * @param gauge the gauge for this screen. Omitted rows keep the slot's width so columns align.
 * @param trailing the trailing value. On [Severity.Stopped] rows this is a [StoppedChip].
 * @param dormant not sold in 14+ days — the name drops to [ShopBlackMid]: visible, never urgent.
 */
@Composable
fun InsightRow(
    name: String,
    meta: String,
    modifier: Modifier = Modifier,
    severity: Severity = Severity.Healthy,
    dormant: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    gauge: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val surface = if (severity == Severity.Warning) StockOrangeSubtle else Color.Transparent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(ROW_RADIUS))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        leading?.invoke()

        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (dormant) ShopBlackMid else ShopBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                fontSize = 11.5.sp,
                color = ShopBlackMid,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // The slot is held even when empty, so gauges line up down the list.
        if (gauge != null) gauge() else Spacer(Modifier.width(GAUGE_WIDTH))

        trailing?.invoke()
    }
}

/**
 * The default trailing slot: a value over a sub-line. Both use `tnum` numerals via the
 * numeric styles, so a changing figure never shifts the layout beside it.
 */
@Composable
fun InsightRowValue(
    value: String,
    sub: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = value,
            style = NumberRowValue,
            color = ShopBlack,
            textAlign = TextAlign.End,
        )
        if (sub != null) {
            Text(
                text = sub,
                style = NumberRowSub,
                color = ShopBlackMid,
                textAlign = TextAlign.End,
            )
        }
    }
}

private val ROW_RADIUS = 12.dp

@Preview(showBackground = true, backgroundColor = 0xFFFDFCFA)
@Composable
private fun InsightRowPreview() {
    ShopMateTheme {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            InsightRow(
                name = "Coca Cola 500ml",
                meta = "Runs out Thursday",
                severity = Severity.Warning,
                gauge = { RunwayGauge(daysOfCover = 3.4, confident = true) },
                trailing = { InsightRowValue(value = "24", sub = "btl") },
            )
            InsightRow(
                name = "Bread Loaf",
                meta = "Out since Tuesday",
                severity = Severity.Stopped,
                gauge = { RunwayGauge(daysOfCover = 0.0, confident = true) },
                trailing = { StoppedChip("Out") },
            )
            InsightRow(
                name = "Milk 1L",
                meta = "Not enough history yet",
                gauge = { RunwayGauge(daysOfCover = null, confident = false) },
                trailing = { InsightRowValue(value = "12", sub = "litre") },
            )
            InsightRow(
                name = "Cooking Oil 2L",
                meta = "Not sold in 21 days",
                dormant = true,
                gauge = { MarginGauge(margin = 0.18, targetMargin = 0.30) },
                trailing = { InsightRowValue(value = "18%", sub = "was 26%") },
            )
        }
    }
}
