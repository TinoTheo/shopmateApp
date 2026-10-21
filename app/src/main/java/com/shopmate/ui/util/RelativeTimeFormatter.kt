package com.shopmate.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Formats an [Instant] as a human-readable relative string:
 * "Just now", "5 minutes ago", "3 hours ago", "Yesterday", "4 days ago",
 * or an absolute date ("29 Jun 2026") once it is a week old.
 */
object RelativeTimeFormatter {

    private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

    fun format(instant: Instant, now: Instant = Instant.now()): String {
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val zone = ZoneId.systemDefault()

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes ${"minute".pluralize(minutes)} ago"
            hours < 24 -> "$hours ${"hour".pluralize(hours)} ago"
            else -> {
                val days = ChronoUnit.DAYS.between(
                    instant.atZone(zone).toLocalDate(),
                    now.atZone(zone).toLocalDate(),
                )
                when {
                    days <= 1L -> "Yesterday"
                    days < 7L -> "$days days ago"
                    else -> dateFormat.format(instant.atZone(zone))
                }
            }
        }
    }

    private fun String.pluralize(count: Long): String = if (count == 1L) this else "${this}s"
}
