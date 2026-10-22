package com.shopmate.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Report models are computed on the fly from local Room data — none of them are persisted.
 * Every value is derived from the `sales` and `sale_items` tables for a given date range.
 */

data class DailySummary(
    val date: LocalDate,
    val totalRevenue: BigDecimal,
    val totalSales: Int,
    val totalItemsSold: Int,
    val averageSaleValue: BigDecimal,
    val paymentBreakdown: Map<PaymentMethod, BigDecimal>,
)

data class TopProduct(
    val productId: String,
    val productName: String,
    val categoryName: String?,
    val categoryColour: String?,
    val quantitySold: Int,
    val revenue: BigDecimal,
    val revenueShare: Float,       // percentage of total revenue (0..100)
)

data class PeriodSummary(
    val periodLabel: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalRevenue: BigDecimal,
    val totalSales: Int,
    val totalItemsSold: Int,
    val averageSaleValue: BigDecimal,
    val dailyRevenues: List<DailyRevenue>,
    val topProducts: List<TopProduct>,
    val paymentBreakdown: Map<PaymentMethod, BigDecimal>,
    val revenueTrend: RevenueTrend,
)

data class DailyRevenue(
    val date: LocalDate,
    val revenue: BigDecimal,
)

data class HourlyRevenue(
    val hour: Int,      // 0-23
    val revenue: BigDecimal,
)

data class RevenueTrend(
    val currentPeriodTotal: BigDecimal,
    val previousPeriodTotal: BigDecimal,
    val percentageChange: Float,      // +12.5 or -8.3
    val direction: TrendDirection,
) {
    /** False when the comparison span sold nothing: "Nothing to compare with yet", never "+100%". */
    val comparable: Boolean get() = previousPeriodTotal.signum() > 0
}

enum class TrendDirection { UP, DOWN, FLAT }
