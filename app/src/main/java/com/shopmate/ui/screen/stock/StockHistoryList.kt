package com.shopmate.ui.screen.stock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.StockDelta

/**
 * Recent movement history for the selected product. Renders inside the (scrollable)
 * adjustment sheet, so it uses a plain Column rather than a nested LazyColumn.
 */
@Composable
fun StockHistoryList(
    history: List<StockDelta>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.stock_recent_history),
            modifier = Modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider()

        if (history.isEmpty()) {
            Text(
                stringResource(R.string.stock_no_movements),
                modifier = Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            history.take(10).forEach { delta ->
                StockDeltaRow(delta = delta)
                HorizontalDivider()
            }
        }
    }
}
