package com.shopmate.ui.screen.stock

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.shopmate.R
import com.shopmate.domain.model.ProductInsight
import com.shopmate.domain.model.RunOut
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

// The words for a RunOut. Every Stock surface goes through these — the list row, the detail
// sheet and the Products dormant line — so the same product never reads two ways.

private val dayMonth = DateTimeFormatter.ofPattern("d MMMM")

/** The list row's meta line, and the detail's first line. */
@Composable
fun RunOut.label(): String = when (this) {
    is RunOut.Out -> stringResource(R.string.runout_out)
    RunOut.Today -> stringResource(R.string.runout_today)
    RunOut.Tomorrow -> stringResource(R.string.runout_tomorrow)
    is RunOut.OnDay -> stringResource(R.string.runout_day, day.getDisplayName(TextStyle.FULL, Locale.getDefault()))
    RunOut.OverAWeek -> stringResource(R.string.runout_week)
    is RunOut.OverWeeks -> pluralStringResource(R.plurals.runout_weeks, weeks, weeks)
    RunOut.OverAMonth -> stringResource(R.string.runout_month)
    is RunOut.NoHistory -> stringResource(R.string.runout_no_history)
    is RunOut.Dormant -> pluralStringResource(R.plurals.runout_dormant, days, days)
}

/** The detail sheet's first line: as [label], except an out item says since when. */
@Composable
fun RunOut.detailLabel(): String = when (this) {
    is RunOut.Out -> stringResource(R.string.runout_out_since, since.format(dayMonth))
    else -> label()
}

/**
 * The detail sheet's second line — where the date comes from — or null when there is nothing
 * to explain (out, over a week, not selling).
 */
@Composable
fun ProductInsight.runOutBasis(): String? = when (val r = runOut) {
    RunOut.Today, RunOut.Tomorrow, is RunOut.OnDay ->
        unitsPerDay?.let { stringResource(R.string.runout_basis, it.perDay()) }
    is RunOut.NoHistory -> pluralStringResource(
        R.plurals.runout_no_history_basis,
        r.soldSinceAdded,
        r.soldSinceAdded,
        r.addedOn.format(dayMonth),
    )
    else -> null
}

/** Whole numbers, or one decimal place below 1 — "about 9 a day", "about 0.4 a day". */
private fun Double.perDay(): String =
    if (this < 1.0) String.format(Locale.getDefault(), "%.1f", this) else roundToInt().toString()

/** "today", "tomorrow" or "on Thursday", for the strip's caption. */
@Composable
fun stripWhen(offset: Int, today: LocalDate = LocalDate.now()): String = when (offset) {
    0 -> stringResource(R.string.stock_week_when_today)
    1 -> stringResource(R.string.stock_week_when_tomorrow)
    else -> stringResource(
        R.string.stock_week_when_day,
        today.plusDays(offset.toLong()).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
    )
}
