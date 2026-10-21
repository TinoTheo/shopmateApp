package com.shopmate.domain.repository

import com.shopmate.domain.model.HourlyRevenue
import com.shopmate.domain.model.PeriodSummary
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
     * Full summary for [startDate]..[endDate], including a revenue trend computed against the
     * equivalent previous period [comparisonStart]..[comparisonEnd].
     */
    fun getPeriodSummary(
        periodLabel: String,
        startDate: LocalDate,
        endDate: LocalDate,
        comparisonStart: LocalDate,
        comparisonEnd: LocalDate,
        topProductLimit: Int = 10,
    ): Flow<PeriodSummary>

    /** Hourly revenue buckets (0..23) for a single day — powers the TODAY chart. */
    fun getHourlyRevenues(date: LocalDate): Flow<List<HourlyRevenue>>
}
