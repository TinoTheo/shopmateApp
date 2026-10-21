package com.shopmate.ui.screen.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.PeriodSummary
import com.shopmate.ui.components.AnimatedCounter
import com.shopmate.ui.components.CardMaxWidth
import com.shopmate.ui.components.TrendIndicator
import com.shopmate.ui.theme.NumberLarge
import com.shopmate.ui.theme.NumberSmall
import com.shopmate.ui.theme.Spacing
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.theme.WarmWhite
import com.shopmate.ui.util.CurrencyFormatter
import kotlin.math.abs

/**
 * The flagship figure. "Earned today" in plain words, the amount typeset large with tabular
 * figures and the cents dimmed so the eye lands on the whole number, then the trend spoken
 * as a sentence rather than a raw percentage.
 */
@Composable
fun RevenueHeroCard(
    summary: PeriodSummary,
    comparisonLabel: String,
    modifier: Modifier = Modifier,
    isToday: Boolean = false,
) {
    val change = summary.revenueTrend.percentageChange
    val trendText = when {
        change > 1f -> stringResource(R.string.report_trend_up, "%.0f%%".format(change))
        change < -1f -> stringResource(R.string.report_trend_down, "%.0f%%".format(abs(change)))
        else -> stringResource(R.string.report_trend_flat)
    }

    Card(
        modifier = modifier.widthIn(max = CardMaxWidth),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TradeGreen, contentColor = WarmWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(
                    if (isToday) R.string.report_earned_today else R.string.report_earned_period,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = WarmWhite.copy(alpha = 0.72f),
            )
            AnimatedCounter(
                target = summary.totalRevenue,
                style = NumberLarge,
                color = WarmWhite,
                dimCents = true,
            )
            TrendIndicator(
                percentageChange = change,
                label = comparisonLabel,
                text = trendText,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MiniStat(value = "${summary.totalSales}", label = "sales")
                MiniStat(value = "${summary.totalItemsSold}", label = "items")
                MiniStat(value = CurrencyFormatter.format(summary.averageSaleValue), label = "average")
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Tabular figures so the three stats sit on a shared rhythm.
        Text(
            text = value,
            style = NumberSmall,
            color = WarmWhite,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = WarmWhite.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}
