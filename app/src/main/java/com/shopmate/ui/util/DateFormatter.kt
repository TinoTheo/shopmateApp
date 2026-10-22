package com.shopmate.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats dates, times and hours for Reports and Sync. */
object DateFormatter {

    private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val fullDate = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
    private val dayOfWeek = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    fun formatShortDate(date: LocalDate): String = shortDate.format(date)        // "29 Jun"
    fun formatFullDate(date: LocalDate): String = fullDate.format(date)          // "29 June 2026"
    fun formatDayOfWeek(date: LocalDate): String = dayOfWeek.format(date)        // "Mon"
    fun formatMonthYear(date: LocalDate): String = monthYear.format(date)        // "June 2026"
    fun formatDayNumber(date: LocalDate): String = date.dayOfMonth.toString()    // "29"

    private val clock = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dayMonthClock = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault())

    /** "15:21" in this till's time zone. */
    fun formatClock(at: Instant): String = clock.format(at.atZone(ZoneId.systemDefault()))

    /** "22 Oct, 15:21". */
    fun formatDayClock(at: Instant): String = dayMonthClock.format(at.atZone(ZoneId.systemDefault()))

    /** "9am", "12pm", "3pm". */
    fun formatHour(hour: Int): String {
        val h = ((hour % 24) + 24) % 24
        val suffix = if (h < 12) "am" else "pm"
        val display = when (h % 12) {
            0 -> 12
            else -> h % 12
        }
        return "$display$suffix"
    }
}
