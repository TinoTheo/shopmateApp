package com.shopmate.ui.screen.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.Product
import com.shopmate.domain.model.ProductInsight
import com.shopmate.ui.components.EmptyState
import com.shopmate.ui.components.InsightRow
import com.shopmate.ui.components.InsightRowValue
import com.shopmate.ui.components.RunwayGauge
import com.shopmate.ui.components.SectionHeader
import com.shopmate.ui.components.Severity
import com.shopmate.ui.components.ShopMateSearchBar
import com.shopmate.ui.components.StoppedChip
import com.shopmate.ui.util.DeviceLayout

/**
 * The Stock list: the week-ahead strip, then four sections of [InsightRow]s carrying a
 * [RunwayGauge]. Same row component as Products — that is what makes the two screens read as
 * siblings (CLAUDE.md § Component library).
 */
@Composable
fun StockInsightList(
    state: StockUiState,
    deviceLayout: DeviceLayout,
    onSearchChange: (String) -> Unit,
    onProductClick: (Product) -> Unit,
    onNavigateToProducts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.products.isEmpty() && state.searchQuery.isBlank()) {
        EmptyState(
            icon = Icons.Outlined.Inventory2,
            title = stringResource(R.string.stock_empty_title),
            subtitle = stringResource(R.string.add_first_product),
            actionText = stringResource(R.string.stock_empty_action),
            onAction = onNavigateToProducts,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    Column(modifier.fillMaxSize()) {
        ShopMateSearchBar(
            query = state.searchQuery,
            onQueryChange = onSearchChange,
            placeholder = stringResource(R.string.stock_search),
            onClear = { onSearchChange("") },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (state.products.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.stock_no_match),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 88.dp),   // clears the extended FAB
            // 1dp between rows: warning rows share a tint, so they must not merge into a block.
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            item {
                WeekAheadStrip(
                    countsByDay = state.weekAhead,
                    summary = weekCaption(state.weekAhead, state.outOfStockCount),
                    deviceLayout = deviceLayout,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            state.sections.forEach { (section, rows) ->
                item(key = "header-${section.name}") {
                    SectionHeader(label = section.label(rows.size))
                }
                items(rows, key = { it.insight.product.id }) { row ->
                    StockInsightRow(row = row, onClick = { onProductClick(row.insight.product) })
                }
            }
        }
    }
}

@Composable
private fun StockInsightRow(row: StockRow, onClick: () -> Unit) {
    val insight = row.insight
    InsightRow(
        name = insight.product.name,
        meta = insight.runOut.label(),
        severity = when (row.section) {
            StockSection.RunsOutThisWeek -> Severity.Warning
            StockSection.Stopped -> Severity.Stopped
            else -> Severity.Healthy
        },
        dormant = row.section == StockSection.NotMoving,
        gauge = {
            RunwayGauge(daysOfCover = insight.daysOfCover, confident = insight.confident)
        },
        trailing = {
            if (insight.outOfStock) {
                StoppedChip(stringResource(R.string.stock_stopped_out))
            } else {
                InsightRowValue(
                    value = "${insight.product.stockQuantity}",
                    sub = insight.product.unit,
                )
            }
        },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun StockSection.label(count: Int): String = stringResource(
    when (this) {
        StockSection.RunsOutThisWeek -> R.string.stock_section_week
        StockSection.Stopped -> R.string.stock_section_out
        StockSection.Healthy -> R.string.stock_section_in
        StockSection.NotMoving -> R.string.stock_section_not_moving
    },
    count,
)

/**
 * "4 items run out in the next 7 days, the first tomorrow. 1 is already out." Names the first
 * day only; the strip above shows the rest.
 */
@Composable
private fun weekCaption(countsByDay: List<Int>, outCount: Int): String {
    val total = countsByDay.sum()
    val head = if (total == 0) {
        stringResource(R.string.stock_week_caption_none)
    } else {
        val first = countsByDay.indexOfFirst { it > 0 }
        pluralStringResource(R.plurals.stock_week_caption_first, total, total, stripWhen(first))
    }
    val tail = if (outCount > 0) " " + pluralStringResource(R.plurals.stock_week_caption_out, outCount, outCount) else ""
    return head + tail
}
