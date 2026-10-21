package com.shopmate.ui.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats [LocalDate]/hour values for the Reports screen. */
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
