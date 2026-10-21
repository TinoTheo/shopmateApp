package com.shopmate.ui.screen.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shopmate.ui.components.ShopMateFilterChip

private data class PeriodChip(val period: ReportPeriod, val label: String)

private val chips = listOf(
    PeriodChip(ReportPeriod.TODAY, "Today"),
    PeriodChip(ReportPeriod.THIS_WEEK, "This Week"),
    PeriodChip(ReportPeriod.THIS_MONTH, "This Month"),
    PeriodChip(ReportPeriod.LAST_MONTH, "Last Month"),
    PeriodChip(ReportPeriod.CUSTOM, "Custom"),
)

@Composable
fun PeriodSelector(
    selected: ReportPeriod,
    onSelect: (ReportPeriod) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            ShopMateFilterChip(
                label = chip.label,
                selected = chip.period == selected,
                onClick = {
                    if (chip.period == ReportPeriod.CUSTOM) onCustomClick() else onSelect(chip.period)
                },
            )
        }
    }
}
