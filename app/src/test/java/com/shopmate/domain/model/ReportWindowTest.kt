package com.shopmate.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ReportWindowTest {

    private val zone = ZoneOffset.ofHours(2)

    /** Thursday 22 October 2026, 15:30. */
    private val now: ZonedDateTime = ZonedDateTime.of(LocalDateTime.of(2026, 10, 22, 15, 30), zone)

    private fun at(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0) =
        ZonedDateTime.of(LocalDateTime.of(y, m, d, h, min), zone).toInstant()

    @Test
    fun `today compares with yesterday up to this time`() {
        val w = ReportWindow.resolve(ReportPeriod.TODAY, now, null)
        assertEquals(LocalDate.of(2026, 10, 22), w.start)
        assertEquals(at(2026, 10, 21), w.comparisonStart)
        assertEquals(at(2026, 10, 21, 15, 30), w.comparisonEnd)
        assertEquals(true, w.open)
    }

    @Test
    fun `this week compares with last week up to the same weekday and time`() {
        val w = ReportWindow.resolve(ReportPeriod.THIS_WEEK, now, null)
        assertEquals(LocalDate.of(2026, 10, 19), w.start)            // Monday
        assertEquals(LocalDate.of(2026, 10, 25), w.periodEnd)        // Sunday
        assertEquals(at(2026, 10, 12), w.comparisonStart)            // last Monday
        assertEquals(at(2026, 10, 15, 15, 30), w.comparisonEnd)      // last Thursday 15:30
        assertEquals(4, w.days)
    }

    @Test
    fun `this month compares with last month up to the same day and time`() {
        val w = ReportWindow.resolve(ReportPeriod.THIS_MONTH, now, null)
        assertEquals(at(2026, 9, 1), w.comparisonStart)
        assertEquals(at(2026, 9, 22, 15, 30), w.comparisonEnd)
        assertEquals(YearMonth.of(2026, 9), w.comparisonMonth)
        assertEquals(LocalDate.of(2026, 10, 31), w.periodEnd)
    }

    @Test
    fun `the same day is clamped to a shorter month`() {
        val march31 = ZonedDateTime.of(LocalDateTime.of(2026, 3, 31, 9, 0), zone)
        val w = ReportWindow.resolve(ReportPeriod.THIS_MONTH, march31, null)
        assertEquals(at(2026, 2, 28, 9, 0), w.comparisonEnd)
    }

    @Test
    fun `last month is closed and compares whole months`() {
        val w = ReportWindow.resolve(ReportPeriod.LAST_MONTH, now, null)
        assertEquals(LocalDate.of(2026, 9, 1), w.start)
        assertEquals(LocalDate.of(2026, 9, 30), w.end)
        assertEquals(false, w.open)
        assertEquals(at(2026, 8, 1), w.comparisonStart)
        assertEquals(at(2026, 9, 1), w.comparisonEnd)
        assertEquals(YearMonth.of(2026, 8), w.comparisonMonth)
    }

    @Test
    fun `picked dates compare with the same number of days just before`() {
        val w = ReportWindow.resolve(ReportPeriod.CUSTOM, now, LocalDate.of(2026, 10, 3) to LocalDate.of(2026, 10, 9))
        assertEquals(7, w.days)
        assertEquals(false, w.open)
        assertEquals(at(2026, 9, 26), w.comparisonStart)
        assertEquals(at(2026, 10, 3), w.comparisonEnd)
    }

    @Test
    fun `picked dates reaching today compare like with like`() {
        val w = ReportWindow.resolve(ReportPeriod.CUSTOM, now, LocalDate.of(2026, 10, 20) to LocalDate.of(2026, 10, 22))
        assertEquals(true, w.open)
        assertEquals(at(2026, 10, 17), w.comparisonStart)
        assertEquals(at(2026, 10, 19, 15, 30), w.comparisonEnd)
    }
}
