package com.shopmate.domain.repository

import com.shopmate.domain.model.HourlyRevenue
import com.shopmate.domain.model.PeriodSummary
import com.shopmate.domain.model.ReportWindow
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Reactive, read-only reporting over local Room data. Every function emits a fresh value whenever
 * the underlying sales change, so the Reports screen never needs a manual refresh.
 *
 * Single-store app — no `store_id` filtering (the schema has no store dimension). Date ranges use
 * inclusive [startDate, endDate] whole days in the device's local time zone.
 */
interface ReportRepository {

    /**
     * Full summary for the [window]'s days, with a revenue trend against the window's comparison
     * span — which for an unfinished period stops at the same time of day (like with like).
     */
    fun getPeriodSummary(
        window: ReportWindow,
        topProductLimit: Int = 10,
    ): Flow<PeriodSummary>

    /** Hourly revenue buckets (0..23) for a single day — powers the TODAY chart. */
    fun getHourlyRevenues(date: LocalDate): Flow<List<HourlyRevenue>>
}
