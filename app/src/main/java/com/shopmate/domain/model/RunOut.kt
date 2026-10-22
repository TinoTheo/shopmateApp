package com.shopmate.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * When a product runs out, in the words the Stock screen uses — computed once per insight and
 * read as-is by the list row, the detail sheet, the week-ahead strip and its caption. None of
 * them recompute or reformat it, which is what keeps them from disagreeing (the list once said
 * "1 days left" while the detail said "0 days left": one rounded, the other truncated).
 *
 * The day is found by comparing **calendar dates** — today vs the date the stock is projected to
 * hit zero — never by rounding a count of days.
 */
sealed interface RunOut {
    /** Already out; [since] is when the last sale took it to zero, as best the ledger knows. */
    data class Out(val since: LocalDate) : RunOut

    data object Today : RunOut

    data object Tomorrow : RunOut

    /** 2–6 calendar days ahead: named by its day. */
    data class OnDay(val day: DayOfWeek, val daysAhead: Int) : RunOut

    /** 7–13 days. */
    data object OverAWeek : RunOut

    /** 14–27 days; [weeks] = days / 7. */
    data class OverWeeks(val weeks: Int) : RunOut

    /** 28 days or more. */
    data object OverAMonth : RunOut

    /** Too little selling to project (§ Velocity confidence rule). Never a guessed day. */
    data class NoHistory(val soldSinceAdded: Int, val addedOn: LocalDate) : RunOut

    /** Not sold in [days] days (14+). Visible, never urgent. */
    data class Dormant(val days: Int) : RunOut

    /** Offset from today for the seven-day strip (0 = today), or null if it isn't on the strip. */
    val stripOffset: Int?
        get() = when (this) {
            Today -> 0
            Tomorrow -> 1
            is OnDay -> daysAhead
            else -> null
        }

    companion object {
        private const val SECONDS_PER_DAY = 86_400.0

        /**
         * Out first (it is already costing money), then not selling, then too little history to
         * project, then a projected day. The day comes from comparing **calendar dates** — today vs
         * the date stock reaches zero at this pace — never from rounding a count of days.
         *
         * [unitsPerDay] is null when history is too thin to project (§ Velocity confidence rule).
         */
        fun of(
            stock: Int,
            unitsPerDay: Double?,
            daysSinceLastSale: Int?,
            lastSaleAt: Instant?,
            unitsSoldAllTime: Int,
            addedAt: Instant,
            updatedAt: Instant,
            now: Instant,
            zone: ZoneId,
        ): RunOut {
            val today = now.atZone(zone).toLocalDate()
            if (stock <= 0) return Out((lastSaleAt ?: updatedAt).atZone(zone).toLocalDate())
            if (daysSinceLastSale != null && daysSinceLastSale >= ProductInsight.DORMANT_DAYS) {
                return Dormant(daysSinceLastSale)
            }
            if (unitsPerDay == null || unitsPerDay <= 0.0) {
                return NoHistory(unitsSoldAllTime, addedAt.atZone(zone).toLocalDate())
            }
            val runOutDate = now.plusSeconds((stock / unitsPerDay * SECONDS_PER_DAY).toLong())
                .atZone(zone).toLocalDate()
            val days = ChronoUnit.DAYS.between(today, runOutDate)
            return when {
                days <= 0 -> Today
                days == 1L -> Tomorrow
                days in 2..6 -> OnDay(runOutDate.dayOfWeek, days.toInt())
                days in 7..13 -> OverAWeek
                days in 14..27 -> OverWeeks((days / 7).toInt())
                else -> OverAMonth
            }
        }
    }
}
