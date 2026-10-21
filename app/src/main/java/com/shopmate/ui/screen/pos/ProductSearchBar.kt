package com.shopmate.ui.screen.pos

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.ui.components.ShopMateSearchBar
import com.shopmate.ui.theme.TradeGreen

@Composable
fun ProductSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShopMateSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = stringResource(R.string.pos_search_products),
        onClear = onClear,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        trailingContent = {
            // Fixed-position barcode trigger — the fastest add path lives in the search
            // bar so it builds muscle memory. Scanner wiring lands in a later phase.
            IconButton(onClick = { /* scanner: later phase */ }, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.QrCodeScanner,
                    contentDescription = stringResource(R.string.pos_scan_barcode),
                    tint = TradeGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
    )
}
