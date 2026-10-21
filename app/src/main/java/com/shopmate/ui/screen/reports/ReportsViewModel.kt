package com.shopmate.ui.screen.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.domain.model.DailyRevenue
import com.shopmate.domain.model.HourlyRevenue
import com.shopmate.domain.model.PeriodSummary
import com.shopmate.domain.repository.ReportRepository
import com.shopmate.ui.util.CurrencyFormatter
import com.shopmate.ui.util.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

enum class ReportPeriod { TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, CUSTOM }

data class ReportBar(
    val label: String,
    val value: Float,
    val tooltip: String,
)

data class ReportChartData(
    val bars: List<ReportBar> = emptyList(),
    val isHourly: Boolean = false,
)

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.TODAY,
    val periodLabel: String = "Today",
    val comparisonLabel: String = "vs yesterday",
    val summary: PeriodSummary? = null,
    val chart: ReportChartData = ReportChartData(),
    val customRange: Pair<LocalDate, LocalDate>? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.TODAY)
    private val _customRange = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)

    /** Overridable for deterministic testing; defaults to the device clock. */
    private fun today(): LocalDate = LocalDate.now()

    val uiState: StateFlow<ReportsUiState> =
        combine(_selectedPeriod, _customRange) { period, custom -> resolveConfig(period, custom) }
            .flatMapLatest { config ->
                val summaryFlow = reportRepository.getPeriodSummary(
                    periodLabel = config.periodLabel,
                    startDate = config.start,
                    endDate = config.end,
                    comparisonStart = config.comparisonStart,
                    comparisonEnd = config.comparisonEnd,
                )
                if (config.period == ReportPeriod.TODAY) {
                    combine(summaryFlow, reportRepository.getHourlyRevenues(config.end)) { summary, hourly ->
                        buildState(config, summary, hourlyBars(hourly))
                    }
                } else {
                    summaryFlow.map { summary ->
                        buildState(config, summary, dailyBars(config, summary.dailyRevenues))
                    }
                }
            }
            .catch { emit(ReportsUiState(isLoading = false, error = it.message ?: "Could not load reports")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun selectPeriod(period: ReportPeriod) {
        if (period == ReportPeriod.CUSTOM && _customRange.value == null) {
            // Wait for a range to be picked before switching; the screen opens the picker.
            _selectedPeriod.value = ReportPeriod.CUSTOM
            return
        }
        _selectedPeriod.value = period
    }

    fun selectCustomRange(start: LocalDate, end: LocalDate) {
        val ordered = if (end.isBefore(start)) end to start else start to end
        _customRange.value = ordered
        _selectedPeriod.value = ReportPeriod.CUSTOM
    }

    private fun buildState(config: PeriodConfig, summary: PeriodSummary, chart: ReportChartData) =
        ReportsUiState(
            selectedPeriod = config.period,
            periodLabel = config.periodLabel,
            comparisonLabel = config.comparisonLabel,
            summary = summary,
            chart = chart,
            customRange = _customRange.value,
            isLoading = false,
            error = null,
        )

    // ---- Chart bar building ----

    private fun hourlyBars(hourly: List<HourlyRevenue>): ReportChartData {
        val bars = hourly.map { hr ->
            val label = DateFormatter.formatHour(hr.hour)
            ReportBar(label, hr.revenue.toFloat(), "$label • ${CurrencyFormatter.format(hr.revenue)}")
        }
        return ReportChartData(bars = bars, isHourly = true)
    }

    private fun dailyBars(config: PeriodConfig, daily: List<DailyRevenue>): ReportChartData {
        val revenueByDate = daily.associate { it.date to it.revenue }
        val days = datesBetween(config.start, config.end)
        val spanDays = days.size

        // CUSTOM ranges longer than a month read better bucketed by week.
        if (config.period == ReportPeriod.CUSTOM && spanDays > 31) {
            val weekFields = WeekFields.of(Locale.getDefault())
            val buckets = LinkedHashMap<LocalDate, BigDecimal>()
            days.forEach { date ->
                val weekStart = date.with(weekFields.dayOfWeek(), 1)
                val revenue = revenueByDate[date] ?: BigDecimal.ZERO
                buckets[weekStart] = (buckets[weekStart] ?: BigDecimal.ZERO).add(revenue)
            }
            val bars = buckets.entries.map { (weekStart, revenue) ->
                val label = DateFormatter.formatShortDate(weekStart)
                ReportBar(label, revenue.toFloat(), "Week of $label • ${CurrencyFormatter.format(revenue)}")
            }
            return ReportChartData(bars = bars, isHourly = false)
        }

        val bars = days.map { date ->
            val revenue = revenueByDate[date] ?: BigDecimal.ZERO
            val label = when (config.period) {
                ReportPeriod.THIS_WEEK -> DateFormatter.formatDayOfWeek(date)
                ReportPeriod.THIS_MONTH, ReportPeriod.LAST_MONTH -> DateFormatter.formatDayNumber(date)
                else -> DateFormatter.formatShortDate(date)
            }
            ReportBar(
                label = label,
                value = revenue.toFloat(),
                tooltip = "${DateFormatter.formatShortDate(date)} • ${CurrencyFormatter.format(revenue)}",
            )
        }
        return ReportChartData(bars = bars, isHourly = false)
    }

    private fun datesBetween(start: LocalDate, end: LocalDate): List<LocalDate> {
        if (end.isBefore(start)) return listOf(start)
        val count = ChronoUnit.DAYS.between(start, end).toInt().coerceAtMost(MAX_DAILY_BARS)
        return (0..count).map { start.plusDays(it.toLong()) }
    }

    // ---- Date-range resolution ----

    private fun resolveConfig(period: ReportPeriod, custom: Pair<LocalDate, LocalDate>?): PeriodConfig {
        val today = today()
        return when (period) {
            ReportPeriod.TODAY -> PeriodConfig(
                period = period,
                start = today,
                end = today,
                comparisonStart = today.minusDays(1),
                comparisonEnd = today.minusDays(1),
                periodLabel = "Today",
                comparisonLabel = "vs yesterday",
            )

            ReportPeriod.THIS_WEEK -> {
                val weekStart = today.with(DayOfWeek.MONDAY)
                PeriodConfig(
                    period = period,
                    start = weekStart,
                    end = today,
                    comparisonStart = weekStart.minusWeeks(1),
                    comparisonEnd = today.minusWeeks(1),
                    periodLabel = "This Week",
                    comparisonLabel = "vs last week",
                )
            }

            ReportPeriod.THIS_MONTH -> {
                val monthStart = today.withDayOfMonth(1)
                PeriodConfig(
                    period = period,
                    start = monthStart,
                    end = today,
                    comparisonStart = monthStart.minusMonths(1),
                    comparisonEnd = today.minusMonths(1),
                    periodLabel = "This Month",
                    comparisonLabel = "vs last month",
                )
            }

            ReportPeriod.LAST_MONTH -> {
                val lastMonthStart = today.minusMonths(1).withDayOfMonth(1)
                val lastMonthEnd = lastMonthStart.plusMonths(1).minusDays(1)
                PeriodConfig(
                    period = period,
                    start = lastMonthStart,
                    end = lastMonthEnd,
                    comparisonStart = lastMonthStart.minusMonths(1),
                    comparisonEnd = lastMonthStart.minusDays(1),
                    periodLabel = DateFormatter.formatMonthYear(lastMonthStart),
                    comparisonLabel = "vs previous month",
                )
            }

            ReportPeriod.CUSTOM -> {
                val range = custom ?: (today to today)
                val (start, end) = range
                val span = ChronoUnit.DAYS.between(start, end)
                val comparisonEnd = start.minusDays(1)
                val comparisonStart = comparisonEnd.minusDays(span)
                PeriodConfig(
                    period = period,
                    start = start,
                    end = end,
                    comparisonStart = comparisonStart,
                    comparisonEnd = comparisonEnd,
                    periodLabel = "${DateFormatter.formatShortDate(start)} – ${DateFormatter.formatShortDate(end)}",
                    comparisonLabel = "vs previous period",
                )
            }
        }
    }

    private data class PeriodConfig(
        val period: ReportPeriod,
        val start: LocalDate,
        val end: LocalDate,
        val comparisonStart: LocalDate,
        val comparisonEnd: LocalDate,
        val periodLabel: String,
        val comparisonLabel: String,
    )

    private companion object {
        const val MAX_DAILY_BARS = 61
    }
}
