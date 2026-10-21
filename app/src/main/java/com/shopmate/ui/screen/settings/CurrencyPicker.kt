package com.shopmate.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shopmate.ui.components.ShopMateFilterChip

private val SYMBOLS = listOf("$", "R", "ZWL", "USh")

/** Row of currency-symbol chips; selected uses the shared green filter-chip style. */
@Composable
fun CurrencyPicker(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SYMBOLS.forEach { symbol ->
            ShopMateFilterChip(
                label = symbol,
                selected = symbol == selected,
                onClick = { onSelect(symbol) },
            )
        }
    }
}
