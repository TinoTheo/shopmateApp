package com.shopmate.ui.screen.sync

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack

/** Renders the sync-history header and rows directly into the screen's LazyColumn (no nested scroll). */
fun LazyListScope.syncEventHistory(
    title: String,
    events: List<SyncEventDisplay>,
) {
    item(key = "history-header") {
        Text(
            text = "$title (${events.size})",
            style = MaterialTheme.typography.titleSmall,
            color = ShopBlack,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    itemsIndexed(events, key = { _, e -> e.id }) { index, event ->
        if (index > 0) {
            HorizontalDivider(thickness = 1.dp, color = ReceiptBeige, modifier = Modifier.fillMaxWidth())
        }
        SyncEventRow(event)
    }
}
