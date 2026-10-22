package com.shopmate.data.repository

import com.shopmate.data.local.dao.CategoryDao
import com.shopmate.data.local.dao.PaymentBreakdownRow
import com.shopmate.data.local.dao.ReportDao
import com.shopmate.data.local.dao.TopProductRow
import com.shopmate.domain.model.DailyRevenue
import com.shopmate.domain.model.HourlyRevenue
import com.shopmate.domain.model.PaymentMethod
import com.shopmate.domain.model.PeriodSummary
import com.shopmate.domain.model.ReportWindow
import com.shopmate.domain.model.RevenueTrend
import com.shopmate.domain.model.TopProduct
import com.shopmate.domain.model.TrendDirection
import com.shopmate.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val reportDao: ReportDao,
    private val categoryDao: CategoryDao,
) : ReportRepository {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    private fun LocalDate.startMillis(): Long =
        atStartOfDay(zone).toInstant().toEpochMilli()

    /** Exclusive upper bound: the very start of the day after [this]. */
    private fun LocalDate.endMillisExclusive(): Long =
        plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun parseDecimal(value: String?): BigDecimal =
        value?.takeIf { it.isNotBlank() }?.let {
            runCatching { BigDecimal(it) }.getOrDefault(BigDecimal.ZERO)
        } ?: BigDecimal.ZERO

    override fun getPeriodSummary(
        window: ReportWindow,
        topProductLimit: Int,
    ): Flow<PeriodSummary> {
        val startDate = window.start
        val endDate = window.end
        val startMs = startDate.startMillis()
        val endMs = endDate.endMillisExclusive()
        // Exact instants, not whole days: an open period stops at the same time of day.
        val prevStartMs = window.comparisonStart.toEpochMilli()
        val prevEndMs = window.comparisonEnd.toEpochMilli()

        val dailyFlow = reportDao.getDailyRevenues(startMs, endMs).map { rows ->
            rows.map { DailyRevenue(LocalDate.parse(it.saleDate), parseDecimal(it.totalRevenue)) }
        }
        val topFlow = reportDao.getTopProducts(startMs, endMs, topProductLimit)
        val paymentFlow = reportDao.getPaymentBreakdown(startMs, endMs)
        val totalsFlow = combine(
            reportDao.getTotalRevenue(startMs, endMs),
            reportDao.getSaleCount(startMs, endMs),
            reportDao.getItemsSold(startMs, endMs),
        ) { revenue, saleCount, itemsSold ->
            Triple(parseDecimal(revenue), saleCount, parseDecimal(itemsSold).toInt())
        }
        val comparisonFlow = combine(
            reportDao.getTotalRevenue(prevStartMs, prevEndMs),
            categoryDao.getAll(),
        ) { prevRevenue, categories ->
            parseDecimal(prevRevenue) to categories.associate { it.name to it.colour }
        }

        return combine(dailyFlow, topFlow, paymentFlow, totalsFlow, comparisonFlow) {
            daily, topRows, paymentRows, totals, comparison ->
            val (totalRevenue, totalSales, totalItems) = totals
            val (previousTotal, colourByCategory) = comparison

            PeriodSummary(
                periodLabel = window.period.name,
                startDate = startDate,
                endDate = endDate,
                totalRevenue = totalRevenue,
                totalSales = totalSales,
                totalItemsSold = totalItems,
                averageSaleValue = averageSaleValue(totalRevenue, totalSales),
                dailyRevenues = daily,
                topProducts = topRows.toTopProducts(totalRevenue, colourByCategory),
                paymentBreakdown = paymentRows.toPaymentBreakdown(),
                revenueTrend = trend(totalRevenue, previousTotal),
            )
        }
    }

    override fun getHourlyRevenues(date: LocalDate): Flow<List<HourlyRevenue>> {
        val startMs = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return reportDao.getHourlyRevenues(startMs, endMs).map { rows ->
            rows.mapNotNull { row ->
                row.hour.toIntOrNull()?.let { HourlyRevenue(it, parseDecimal(row.totalRevenue)) }
            }
        }
    }

    private fun averageSaleValue(totalRevenue: BigDecimal, totalSales: Int): BigDecimal =
        if (totalSales <= 0) BigDecimal.ZERO
        else totalRevenue.divide(BigDecimal(totalSales), 2, RoundingMode.HALF_UP)

    private fun List<TopProductRow>.toTopProducts(
        totalRevenue: BigDecimal,
        colourByCategory: Map<String, String>,
    ): List<TopProduct> = map { row ->
        val revenue = parseDecimal(row.totalRevenue)
        val share = if (totalRevenue.signum() == 0) 0f
        else revenue.divide(totalRevenue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100)).toFloat()
        TopProduct(
            productId = row.productId,
            productName = row.productName,
            categoryName = row.categoryName,
            categoryColour = row.categoryName?.let { colourByCategory[it] },
            quantitySold = parseDecimal(row.totalQty).toInt(),
            revenue = revenue,
            revenueShare = share,
        )
    }

    private fun List<PaymentBreakdownRow>.toPaymentBreakdown(): Map<PaymentMethod, BigDecimal> =
        mapNotNull { row ->
            runCatching { PaymentMethod.valueOf(row.paymentMethod) }.getOrNull()
                ?.let { it to parseDecimal(row.total) }
        }.toMap()

    private fun trend(current: BigDecimal, previous: BigDecimal): RevenueTrend {
        val change: Float = when {
            previous.signum() == 0 && current.signum() == 0 -> 0f
            previous.signum() == 0 -> 100f
            else -> current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100)).toFloat()
        }
        val direction = when {
            change > 1f -> TrendDirection.UP
            change < -1f -> TrendDirection.DOWN
            else -> TrendDirection.FLAT
        }
        return RevenueTrend(current, previous, change, direction)
    }
}
