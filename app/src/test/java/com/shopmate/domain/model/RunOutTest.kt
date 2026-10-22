package com.shopmate.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class RunOutTest {

    private val zone: ZoneId = ZoneOffset.ofHours(2) // Harare / Johannesburg
    private val added = Instant.parse("2026-09-01T08:00:00Z")

    /** Thursday 22 October 2026, at the given local hour. */
    private fun thursdayAt(hour: Int, minute: Int = 0): Instant =
        LocalDate.of(2026, 10, 22).atTime(hour, minute).atZone(zone).toInstant()

    private fun runOut(
        stock: Int,
        unitsPerDay: Double?,
        now: Instant,
        daysSinceLastSale: Int? = 0,
        lastSaleAt: Instant? = now,
        soldAllTime: Int = 0,
    ) = RunOut.of(stock, unitsPerDay, daysSinceLastSale, lastSaleAt, soldAllTime, added, added, now, zone)

    @Test
    fun `half a day of cover at 9pm runs out tomorrow, not today`() {
        // 12 hours from 21:00 is 09:00 the next day. A count of days (0.5) would round to 0 or 1
        // depending on who rounds; the calendar says tomorrow.
        assertEquals(RunOut.Tomorrow, runOut(stock = 5, unitsPerDay = 10.0, now = thursdayAt(21)))
    }

    @Test
    fun `half a day of cover at 8am runs out today`() {
        assertEquals(RunOut.Today, runOut(stock = 5, unitsPerDay = 10.0, now = thursdayAt(8)))
    }

    @Test
    fun `two to six days ahead is named by its day`() {
        // 3.5 days from Thursday 08:00 is Sunday 20:00.
        assertEquals(RunOut.OnDay(DayOfWeek.SUNDAY, 3), runOut(stock = 35, unitsPerDay = 10.0, now = thursdayAt(8)))
    }

    @Test
    fun `a week or more is said in weeks and months, never a day`() {
        val now = thursdayAt(8)
        assertEquals(RunOut.OverAWeek, runOut(stock = 100, unitsPerDay = 10.0, now = now))      // 10 days
        assertEquals(RunOut.OverWeeks(2), runOut(stock = 200, unitsPerDay = 10.0, now = now))   // 20 days
        assertEquals(RunOut.OverWeeks(3), runOut(stock = 270, unitsPerDay = 10.0, now = now))   // 27 days
        assertEquals(RunOut.OverAMonth, runOut(stock = 400, unitsPerDay = 10.0, now = now))     // 40 days
    }

    @Test
    fun `out beats everything and says since when`() {
        val lastSale = thursdayAt(8).minusSeconds(3 * 86_400L)
        assertEquals(
            RunOut.Out(LocalDate.of(2026, 10, 19)),
            runOut(stock = 0, unitsPerDay = 10.0, now = thursdayAt(8), daysSinceLastSale = 3, lastSaleAt = lastSale),
        )
    }

    @Test
    fun `not sold in 14 days is dormant even with a velocity`() {
        assertEquals(RunOut.Dormant(18), runOut(stock = 14, unitsPerDay = 0.6, now = thursdayAt(8), daysSinceLastSale = 18))
    }

    @Test
    fun `thin history never projects a day`() {
        assertEquals(
            RunOut.NoHistory(soldSinceAdded = 4, addedOn = LocalDate.of(2026, 9, 1)),
            runOut(stock = 10, unitsPerDay = null, now = thursdayAt(8), soldAllTime = 4),
        )
    }

    @Test
    fun `the strip offset matches the day it runs out`() {
        assertEquals(0, RunOut.Today.stripOffset)
        assertEquals(1, RunOut.Tomorrow.stripOffset)
        assertEquals(3, RunOut.OnDay(DayOfWeek.SUNDAY, 3).stripOffset)
        assertEquals(null, RunOut.OverAWeek.stripOffset)
    }
}
