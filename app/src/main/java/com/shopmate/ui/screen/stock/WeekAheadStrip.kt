package com.shopmate.ui.screen.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.R
import com.shopmate.ui.theme.NumberRowSub
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackMid
import com.shopmate.ui.theme.ShopMateTheme
import com.shopmate.ui.theme.StockOrange
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.theme.WarmSand
import com.shopmate.ui.util.DeviceLayout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * The week ahead, in items running out per day — the Stock screen's answer to "when do I run
 * out?" before any row is read.
 *
 * Seven columns; bars are **bottom-anchored** in a [WarmSand] rail so their heights compare
 * across days rather than each floating in its own box. A day with items running out is
 * [StockOrange]; the last day of the window is [ShopBlack] so the horizon reads as a wall.
 *
 * The summary line beneath is deliberately specific — "Six items run out Wednesday and
 * Thursday" beats "Low stock" (§ Copy and voice).
 */
@Composable
fun WeekAheadStrip(
    /** Items running out, indexed by offset from today: index 0 is today. Always 7 entries. */
    countsByDay: List<Int>,
    summary: String,
    deviceLayout: DeviceLayout,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val trackHeight = if (deviceLayout == DeviceLayout.COMPACT) 44.dp else 56.dp
    val peak = (countsByDay.maxOrNull() ?: 0).coerceAtLeast(1)

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            countsByDay.take(DAYS).forEachIndexed { offset, count ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
                            .background(WarmSand, RoundedCornerShape(6.dp)),
                        // Bottom-anchored: a taller bar always means more items.
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(trackHeight * (count.toFloat() / peak))
                                    .background(
                                        barColour(offset = offset, count = count),
                                        RoundedCornerShape(6.dp),
                                    ),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        // The window starts today, so the first column says so.
                        text = if (offset == 0) {
                            stringResource(R.string.reports_chip_today)
                        } else {
                            today.plusDays(offset.toLong()).dayOfWeek.shortLabel()
                        },
                        fontSize = 10.sp,
                        fontWeight = if (offset == 0) FontWeight.Medium else FontWeight.Normal,
                        color = ShopBlackMid,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = if (count > 0) "$count" else "",
                        style = NumberRowSub,
                        color = ShopBlackMid,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(text = summary, fontSize = 12.sp, color = ShopBlackMid)
    }
}

/** The horizon day reads as a wall; every other day with items on it is a Warning. */
private fun barColour(offset: Int, count: Int) = when {
    count == 0 -> TradeGreen
    offset == DAYS - 1 -> ShopBlack
    else -> StockOrange
}

private const val DAYS = 7

private fun DayOfWeek.shortLabel(): String =
    getDisplayName(TextStyle.SHORT, Locale.getDefault())

@Preview(showBackground = true, backgroundColor = 0xFFFDFCFA, widthDp = 390)
@Composable
private fun WeekAheadStripPreview() {
    ShopMateTheme {
        WeekAheadStrip(
            countsByDay = listOf(0, 0, 4, 2, 0, 1, 0),
            summary = "7 items run out in the next 7 days, the first on Saturday. 1 is already out.",
            deviceLayout = DeviceLayout.COMPACT,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}
