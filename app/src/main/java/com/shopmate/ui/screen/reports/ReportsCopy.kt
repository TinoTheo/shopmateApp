package com.shopmate.ui.screen.reports

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.shopmate.R
import com.shopmate.domain.model.ReportPeriod
import com.shopmate.domain.model.ReportWindow
import com.shopmate.domain.model.RevenueTrend
import com.shopmate.domain.model.TrendDirection
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Reports copy, worded by period (copy-pass handoff § 5). Each period names its own comparison,
// and an unfinished one says "so far" — never a partial figure that reads as final.

private val locale: Locale get() = Locale.getDefault()
private val dayMonth = DateTimeFormatter.ofPattern("d MMMM")
private val dayShortMonth = DateTimeFormatter.ofPattern("d MMM")
private val weekdayDay = DateTimeFormatter.ofPattern("EEEE d")
private val weekdayDayMonth = DateTimeFormatter.ofPattern("EEEE d MMMM")

/** "Good afternoon" — by the hour, and with no name: the store name is already in the header. */
@Composable
fun reportsGreeting(now: LocalTime = LocalTime.now()): String = stringResource(
    when {
        now.hour < 12 -> R.string.reports_greeting_morning
        now.hour < 17 -> R.string.reports_greeting_afternoon
        else -> R.string.reports_greeting_evening
    },
)

/** "Thursday 22 October, so far" · "1 to 30 September" · "3 to 9 October". */
@Composable
fun ReportWindow.rangeText(): String {
    val text = when (period) {
        ReportPeriod.TODAY -> start.format(weekdayDayMonth)
        ReportPeriod.THIS_WEEK -> range(start, end, startFormat = if (start.month == end.month) weekdayDay else weekdayDayMonth, endFormat = weekdayDayMonth)
        else -> range(start, end, dayMonth)
    }
    return if (open) stringResource(R.string.reports_range_so_far, text) else text
}

@Composable
private fun range(
    start: LocalDate,
    end: LocalDate,
    endFormat: DateTimeFormatter,
    startFormat: DateTimeFormatter = if (start.month == end.month && start.year == end.year) {
        DateTimeFormatter.ofPattern("d")
    } else {
        endFormat
    },
): String = stringResource(R.string.reports_range, start.format(startFormat), end.format(endFormat))

/** "Sales today" · "Sales this week" · "Sales in October" · "Sales, 3 to 9 Oct". */
@Composable
fun ReportWindow.heroLabel(): String = when (period) {
    ReportPeriod.TODAY -> stringResource(R.string.reports_hero_today)
    ReportPeriod.THIS_WEEK -> stringResource(R.string.reports_hero_week)
    ReportPeriod.THIS_MONTH, ReportPeriod.LAST_MONTH -> stringResource(R.string.reports_hero_month, start.monthName())
    ReportPeriod.CUSTOM -> stringResource(R.string.reports_hero_range, range(start, periodEnd, dayShortMonth))
}

/** The trend in words, against this period's own comparison. */
@Composable
fun ReportWindow.trendText(trend: RevenueTrend): String {
    if (!trend.comparable) return stringResource(R.string.reports_trend_none)
    val pct = "${abs(trend.percentageChange).roundToInt()}%"
    return when (trend.direction) {
        TrendDirection.UP -> trendSentence(up = true, pct)
        TrendDirection.DOWN -> trendSentence(up = false, pct)
        // "Level with yesterday by this time". The phrase after "on" in the matching trend
        // sentence is the comparison's name; the copy has no separate key for it.
        TrendDirection.FLAT -> {
            val marker = "\u0000"
            val sentence = trendSentence(up = true, marker)
            val phrase = sentence.substringAfter("$marker ").substringAfter(' ')
            stringResource(R.string.reports_trend_level, phrase)
        }
    }
}

@Composable
private fun ReportWindow.trendSentence(up: Boolean, pct: String): String = when (period) {
    ReportPeriod.TODAY -> stringResource(if (up) R.string.reports_trend_up_today else R.string.reports_trend_down_today, pct)
    ReportPeriod.THIS_WEEK -> stringResource(
        if (up) R.string.reports_trend_up_week else R.string.reports_trend_down_week,
        pct,
        end.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
    )
    ReportPeriod.THIS_MONTH -> stringResource(
        if (up) R.string.reports_trend_up_month else R.string.reports_trend_down_month,
        pct,
        comparisonMonth?.monthName().orEmpty(),
        ordinal(end.dayOfMonth),
    )
    ReportPeriod.LAST_MONTH -> stringResource(
        if (up) R.string.reports_trend_up_closed_month else R.string.reports_trend_down_closed_month,
        pct,
        comparisonMonth?.monthName().orEmpty(),
    )
    ReportPeriod.CUSTOM -> pluralStringResource(
        if (up) R.plurals.reports_trend_up_range else R.plurals.reports_trend_down_range,
        days,
        days,
        pct,
    )
}

/** "24 sold today" · "6 sold in September". */
@Composable
fun ReportWindow.soldText(quantity: Int): String = when (period) {
    ReportPeriod.TODAY -> pluralStringResource(R.plurals.reports_sold_today, quantity, quantity)
    ReportPeriod.THIS_WEEK -> pluralStringResource(R.plurals.reports_sold_week, quantity, quantity)
    ReportPeriod.THIS_MONTH, ReportPeriod.LAST_MONTH ->
        pluralStringResource(R.plurals.reports_sold_month, quantity, quantity, start.monthName())
    // No range-specific key: "7 sold in 3 to 9 October".
    ReportPeriod.CUSTOM -> pluralStringResource(R.plurals.reports_sold_month, quantity, quantity, range(start, periodEnd, dayMonth))
}

/** Open periods say "yet"; closed ones don't. */
@Composable
fun ReportWindow.emptyText(): String = when (period) {
    ReportPeriod.TODAY -> stringResource(R.string.reports_empty_today)
    ReportPeriod.THIS_WEEK -> stringResource(R.string.reports_empty_week)
    ReportPeriod.THIS_MONTH, ReportPeriod.LAST_MONTH -> stringResource(R.string.reports_empty_closed, start.monthName())
    ReportPeriod.CUSTOM -> stringResource(R.string.reports_empty_range)
}

private fun LocalDate.monthName(): String = month.getDisplayName(TextStyle.FULL, locale)

private fun YearMonth.monthName(): String = month.getDisplayName(TextStyle.FULL, locale)

/** "1st", "2nd", "22nd", "11th". English ordinals, as the copy is English. */
private fun ordinal(day: Int): String = day.toString() + when {
    day % 100 in 11..13 -> "th"
    day % 10 == 1 -> "st"
    day % 10 == 2 -> "nd"
    day % 10 == 3 -> "rd"
    else -> "th"
}
