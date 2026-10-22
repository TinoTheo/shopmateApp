package com.shopmate.ui.screen.reports

import android.graphics.Typeface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.shopmate.R
import com.shopmate.ui.theme.ChartGrid
import com.shopmate.ui.theme.NumberStyle
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.CurrencyFormatter
import java.math.BigDecimal

private const val CHART_HEIGHT_DP = 220

/** The column still filling — this hour, or today — is drawn at this alpha. */
private const val PARTIAL_ALPHA = 0.38f

/**
 * Column chart of revenue over the selected period, in Trade Green with 3dp rounded tops. The
 * column still filling ([ReportChartData.partialIndex]) is lighter, with a caption saying so, so a
 * half-finished hour is never read as a slow one. Days still to come are empty slots.
 */
@Composable
fun RevenueChart(chart: ReportChartData, emptyText: String, modifier: Modifier = Modifier) {
    if (chart.bars.isEmpty() || chart.bars.all { it.value == 0f }) {
        EmptyChart(emptyText, modifier)
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    // Labels and the partial column travel inside the model, in the same transaction as the bars.
    // Read from Compose state they changed the moment the period did, while the chart still held
    // the old bars — switching Today → This week asked for label 12 of 7, got "", and Vico threw.
    LaunchedEffect(chart) {
        modelProducer.runTransaction {
            columnSeries { series(chart.bars.map { it.value }) }
            extras {
                it[BarLabelsKey] = chart.bars.map { bar -> bar.label }
                it[PartialIndexKey] = chart.partialIndex ?: -1
            }
        }
    }

    val barShape = remember { CorneredShape.rounded(3f, 3f, 0f, 0f) }
    val column = rememberLineComponent(fill = fill(TradeGreen), thickness = 10.dp, shape = barShape)
    val partialColumn = rememberLineComponent(
        fill = fill(TradeGreen.copy(alpha = PARTIAL_ALPHA)),
        thickness = 10.dp,
        shape = barShape,
    )
    val columnProvider = remember(column, partialColumn) { PartialColumnProvider(column, partialColumn) }

    val context = LocalContext.current
    val inter = remember { ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.DEFAULT }
    val axisLabel = rememberAxisLabelComponent(color = ShopBlackTertiary, typeface = inter, textSize = 10.5.sp)
    val axisLine = rememberAxisLineComponent(fill = fill(ReceiptBeige))
    val guideline = rememberAxisGuidelineComponent(fill = fill(ChartGrid), thickness = 1.dp, shape = Shape.Rectangle)

    val bottomFormatter = remember {
        CartesianValueFormatter { context, x, _ ->
            // Vico rejects an empty label; a bar without one gets a blank space instead.
            context.model.extraStore.getOrNull(BarLabelsKey)
                ?.getOrNull(x.toInt())
                ?.takeIf { it.isNotEmpty() }
                ?: " "
        }
    }
    val startFormatter = remember {
        CartesianValueFormatter { _, y, _ -> CurrencyFormatter.format(BigDecimal.valueOf(y)) }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(columnProvider = columnProvider),
                startAxis = VerticalAxis.rememberStart(
                    line = axisLine,
                    label = axisLabel,
                    valueFormatter = startFormatter,
                    guideline = guideline,
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    line = axisLine,
                    label = axisLabel,
                    valueFormatter = bottomFormatter,
                    guideline = null,
                ),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT_DP.dp),
        )
        if (chart.partialIndex != null) {
            Text(
                text = stringResource(
                    if (chart.isHourly) R.string.reports_chart_partial_hour else R.string.reports_chart_partial_day,
                ),
                style = NumberStyle(11.5.sp),
                color = ShopBlackTertiary,
            )
        }
    }
}

/** Trade Green columns, except the one still filling, which is lighter. */
private class PartialColumnProvider(
    private val column: LineComponent,
    private val partialColumn: LineComponent,
) : ColumnCartesianLayer.ColumnProvider {
    override fun getColumn(
        entry: ColumnCartesianLayerModel.Entry,
        seriesIndex: Int,
        extraStore: ExtraStore,
    ): LineComponent =
        if (entry.x.toInt() == extraStore.getOrNull(PartialIndexKey)) partialColumn else column

    override fun getWidestSeriesColumn(seriesIndex: Int, extraStore: ExtraStore): LineComponent = column
}

private val BarLabelsKey = ExtraStore.Key<List<String>>()
private val PartialIndexKey = ExtraStore.Key<Int>()

@Composable
private fun EmptyChart(text: String, modifier: Modifier = Modifier) {
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
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
