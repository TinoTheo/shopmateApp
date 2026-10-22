package com.shopmate.ui.screen.reports

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.ReportPeriod
import com.shopmate.ui.components.ShopMateFilterChip

private data class PeriodChip(val period: ReportPeriod, @StringRes val label: Int)

private val chips = listOf(
    PeriodChip(ReportPeriod.TODAY, R.string.reports_chip_today),
    PeriodChip(ReportPeriod.THIS_WEEK, R.string.reports_chip_week),
    PeriodChip(ReportPeriod.THIS_MONTH, R.string.reports_chip_month),
    PeriodChip(ReportPeriod.LAST_MONTH, R.string.reports_chip_last_month),
    PeriodChip(ReportPeriod.CUSTOM, R.string.reports_chip_pick),
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
                label = stringResource(chip.label),
                selected = chip.period == selected,
                onClick = {
                    if (chip.period == ReportPeriod.CUSTOM) onCustomClick() else onSelect(chip.period)
                },
            )
        }
    }
}
