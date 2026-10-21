package com.shopmate.ui.screen.reports

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.shopmate.ui.util.CurrencyFormatter
import java.math.BigDecimal

private const val CHART_HEIGHT_DP = 220

/**
 * Column (bar) chart of revenue over the selected period. Column colour comes from the M3 theme
 * (`primary` = Trade Green) via the Vico `compose-m3` integration, so no manual styling is needed.
 * Vico animates the bars natively when the data changes.
 */
@Composable
fun RevenueChart(chart: ReportChartData, modifier: Modifier = Modifier) {
    if (chart.bars.isEmpty()) {
        EmptyChart(modifier)
        return
    }

    val labels = remember(chart) { chart.bars.map { it.label } }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(chart) {
        modelProducer.runTransaction {
            columnSeries { series(chart.bars.map { it.value }) }
        }
    }

    val bottomFormatter = remember(labels) {
        CartesianValueFormatter { _, x, _ -> labels.getOrNull(x.toInt()).orEmpty() }
    }
    val startFormatter = remember {
        CartesianValueFormatter { _, y, _ -> CurrencyFormatter.format(BigDecimal.valueOf(y)) }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(valueFormatter = startFormatter),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter),
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT_DP.dp),
    )
}

@Composable
private fun EmptyChart(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Outlined.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "No revenue to chart yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
