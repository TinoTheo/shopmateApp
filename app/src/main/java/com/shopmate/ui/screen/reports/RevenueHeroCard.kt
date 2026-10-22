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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.PeriodSummary
import com.shopmate.domain.model.ReportWindow
import com.shopmate.ui.components.AnimatedCounter
import com.shopmate.ui.components.CardMaxWidth
import com.shopmate.ui.components.TrendIndicator
import com.shopmate.ui.theme.NumberLarge
import com.shopmate.ui.theme.NumberSmall
import com.shopmate.ui.theme.Spacing
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.theme.WarmWhite
import com.shopmate.ui.util.CurrencyFormatter

/**
 * The flagship figure. "Earned today" in plain words, the amount typeset large with tabular
 * figures and the cents dimmed so the eye lands on the whole number, then the trend spoken
 * as a sentence rather than a raw percentage.
 */
@Composable
fun RevenueHeroCard(
    summary: PeriodSummary,
    window: ReportWindow,
    modifier: Modifier = Modifier,
) {
    // Nothing to compare with: no arrow, so the line makes no claim either way.
    val change = if (summary.revenueTrend.comparable) summary.revenueTrend.percentageChange else 0f

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
                text = window.heroLabel(),
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
                label = "",
                text = window.trendText(summary.revenueTrend),
                modifier = Modifier.padding(top = Spacing.xs),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MiniStat(
                    value = "${summary.totalSales}",
                    label = pluralStringResource(R.plurals.reports_stat_sales, summary.totalSales),
                )
                MiniStat(
                    value = "${summary.totalItemsSold}",
                    label = pluralStringResource(R.plurals.reports_stat_items, summary.totalItemsSold),
                )
                MiniStat(
                    value = CurrencyFormatter.format(summary.averageSaleValue),
                    label = stringResource(R.string.reports_stat_average),
                )
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
