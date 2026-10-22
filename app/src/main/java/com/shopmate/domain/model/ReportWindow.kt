package com.shopmate.domain.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

enum class ReportPeriod { TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, CUSTOM }

/**
 * What a Reports period covers and what it is compared with.
 *
 * An unfinished period ([open]) is compared **like with like**: today against yesterday up to the
 * same time of day, this week against last week up to the same weekday and time, this month
 * against last month up to the same day and time. Comparing a partial period with a full one
 * would say "down 60%" every morning (§ Never show a number as certain when it isn't).
 *
 * @property start first day covered, inclusive.
 * @property end last day covered, inclusive — today for an open period.
 * @property periodEnd the period's last day even if it hasn't happened yet (Sunday, month end),
 *   so the chart can show the days still to come.
 * @property comparisonStart inclusive; [comparisonEnd] exclusive.
 * @property comparisonMonth the month compared with, for "on September by the 22nd".
 */
data class ReportWindow(
    val period: ReportPeriod,
    val start: LocalDate,
    val end: LocalDate,
    val periodEnd: LocalDate,
    val open: Boolean,
    val comparisonStart: Instant,
    val comparisonEnd: Instant,
    val comparisonMonth: YearMonth?,
    /** Days covered, for "on the 7 days before". */
    val days: Int,
) {
    companion object {
        fun resolve(period: ReportPeriod, now: ZonedDateTime, custom: Pair<LocalDate, LocalDate>?): ReportWindow {
            val zone = now.zone
            val today = now.toLocalDate()
            val sinceMidnight = Duration.between(today.atStartOfDay(zone), now)
            fun LocalDate.at(offset: Duration = Duration.ZERO): Instant = atStartOfDay(zone).plus(offset).toInstant()

            return when (period) {
                ReportPeriod.TODAY -> {
                    val yesterday = today.minusDays(1)
                    ReportWindow(
                        period, today, today, today, open = true,
                        comparisonStart = yesterday.at(),
                        comparisonEnd = yesterday.at(sinceMidnight),
                        comparisonMonth = null, days = 1,
                    )
                }

                ReportPeriod.THIS_WEEK -> {
                    val monday = today.with(DayOfWeek.MONDAY)
                    val sinceMonday = Duration.between(monday.atStartOfDay(zone), now)
                    val lastMonday = monday.minusWeeks(1)
                    ReportWindow(
                        period, monday, today, monday.plusDays(6), open = true,
                        comparisonStart = lastMonday.at(),
                        comparisonEnd = lastMonday.at(sinceMonday),
                        comparisonMonth = null,
                        days = ChronoUnit.DAYS.between(monday, today).toInt() + 1,
                    )
                }

                ReportPeriod.THIS_MONTH -> {
                    val first = today.withDayOfMonth(1)
                    val previous = YearMonth.from(today).minusMonths(1)
                    // Same day of last month, clamped to its length (31 March → 28/29 February).
                    val sameDay = previous.atDay(minOf(today.dayOfMonth, previous.lengthOfMonth()))
                    ReportWindow(
                        period, first, today, YearMonth.from(today).atEndOfMonth(), open = true,
                        comparisonStart = previous.atDay(1).at(),
                        comparisonEnd = sameDay.at(sinceMidnight),
                        comparisonMonth = previous,
                        days = today.dayOfMonth,
                    )
                }

                ReportPeriod.LAST_MONTH -> {
                    val month = YearMonth.from(today).minusMonths(1)
                    val before = month.minusMonths(1)
                    ReportWindow(
                        period, month.atDay(1), month.atEndOfMonth(), month.atEndOfMonth(), open = false,
                        comparisonStart = before.atDay(1).at(),
                        comparisonEnd = month.atDay(1).at(),
                        comparisonMonth = before,
                        days = month.lengthOfMonth(),
                    )
                }

                ReportPeriod.CUSTOM -> {
                    val (start, end) = custom ?: (today to today)
                    val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
                    // A range reaching today is still open: compare the same span up to now.
                    val open = !end.isBefore(today)
                    val coveredEnd = if (open) today else end
                    val before = start.minusDays(days.toLong())
                    ReportWindow(
                        period, start, coveredEnd, end, open = open,
                        comparisonStart = before.at(),
                        // Open: the same span of time from the earlier start as has passed since this one.
                        comparisonEnd = if (open) before.at(Duration.between(start.atStartOfDay(zone), now)) else start.at(),
                        comparisonMonth = null,
                        days = days,
                    )
                }
            }
        }
    }
}
