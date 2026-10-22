package com.shopmate.ui.screen.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.domain.model.DailyRevenue
import com.shopmate.domain.model.HourlyRevenue
import com.shopmate.domain.model.PeriodSummary
import com.shopmate.domain.model.ReportPeriod
import com.shopmate.domain.model.ReportWindow
import com.shopmate.domain.repository.ReportRepository
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
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

/** One chart column. A [future] day hasn't happened yet: an empty slot, never a zero. */
data class ReportBar(
    val label: String,
    val value: Float,
    val future: Boolean = false,
)

/**
 * [partialIndex] is the column still filling — this hour on Today, today on a week or month —
 * drawn lighter so it isn't read as a finished, low figure.
 */
data class ReportChartData(
    val bars: List<ReportBar> = emptyList(),
    val isHourly: Boolean = false,
    val partialIndex: Int? = null,
)

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.TODAY,
    /** What the period covers and what it's compared with; the screen words it. */
    val window: ReportWindow? = null,
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

    val uiState: StateFlow<ReportsUiState> =
        combine(_selectedPeriod, _customRange) { period, custom ->
            ReportWindow.resolve(period, ZonedDateTime.now(), custom)
        }
            .flatMapLatest { window ->
                val summaryFlow = reportRepository.getPeriodSummary(window)
                if (window.period == ReportPeriod.TODAY) {
                    combine(summaryFlow, reportRepository.getHourlyRevenues(window.end)) { summary, hourly ->
                        buildState(window, summary, hourlyBars(hourly))
                    }
                } else {
                    summaryFlow.map { summary -> buildState(window, summary, dailyBars(window, summary.dailyRevenues)) }
                }
            }
            .catch { emit(ReportsUiState(isLoading = false, error = it.message)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun selectPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun selectCustomRange(start: LocalDate, end: LocalDate) {
        _customRange.value = if (end.isBefore(start)) end to start else start to end
        _selectedPeriod.value = ReportPeriod.CUSTOM
    }

    private fun buildState(window: ReportWindow, summary: PeriodSummary, chart: ReportChartData) =
        ReportsUiState(
            selectedPeriod = window.period,
            window = window,
            summary = summary,
            chart = chart,
            customRange = _customRange.value,
            isLoading = false,
        )

    // ---- Chart bars ----

    private fun hourlyBars(hourly: List<HourlyRevenue>): ReportChartData {
        val bars = hourly.map { ReportBar(DateFormatter.formatHour(it.hour), it.revenue.toFloat()) }
        val currentHour = ZonedDateTime.now().hour
        return ReportChartData(
            bars = bars,
            isHourly = true,
            partialIndex = hourly.indexOfFirst { it.hour == currentHour }.takeIf { it >= 0 },
        )
    }

    private fun dailyBars(window: ReportWindow, daily: List<DailyRevenue>): ReportChartData {
        val revenueByDate = daily.associate { it.date to it.revenue }
        val today = LocalDate.now()
        // Through the period's last day, so the days still to come show as empty slots.
        val days = datesBetween(window.start, window.periodEnd)

        if (window.period == ReportPeriod.CUSTOM && days.size > 31) {
            // Long ranges read better bucketed by week.
            val weekFields = WeekFields.of(Locale.getDefault())
            val buckets = LinkedHashMap<LocalDate, BigDecimal>()
            days.filterNot { it.isAfter(today) }.forEach { date ->
                val weekStart = date.with(weekFields.dayOfWeek(), 1)
                buckets[weekStart] = (buckets[weekStart] ?: BigDecimal.ZERO).add(revenueByDate[date] ?: BigDecimal.ZERO)
            }
            return ReportChartData(
                bars = buckets.map { (weekStart, revenue) -> ReportBar(DateFormatter.formatShortDate(weekStart), revenue.toFloat()) },
            )
        }

        val bars = days.map { date ->
            ReportBar(
                label = when (window.period) {
                    ReportPeriod.THIS_WEEK -> DateFormatter.formatDayOfWeek(date)
                    ReportPeriod.THIS_MONTH, ReportPeriod.LAST_MONTH -> DateFormatter.formatDayNumber(date)
                    else -> DateFormatter.formatShortDate(date)
                },
                value = (revenueByDate[date] ?: BigDecimal.ZERO).toFloat(),
                future = date.isAfter(today),
            )
        }
        return ReportChartData(
            bars = bars,
            partialIndex = if (window.open) days.indexOf(today).takeIf { it >= 0 } else null,
        )
    }

    private fun datesBetween(start: LocalDate, end: LocalDate): List<LocalDate> {
        if (end.isBefore(start)) return listOf(start)
        val count = ChronoUnit.DAYS.between(start, end).toInt().coerceAtMost(MAX_DAILY_BARS)
        return (0..count).map { start.plusDays(it.toLong()) }
    }

    private companion object {
        const val MAX_DAILY_BARS = 61
    }
}
