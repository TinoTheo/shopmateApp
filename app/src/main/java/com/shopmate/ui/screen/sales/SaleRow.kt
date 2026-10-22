package com.shopmate.ui.screen.sales

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.Sale
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.StockGreen
import com.shopmate.ui.util.CurrencyFormatter
import com.shopmate.ui.util.RelativeTimeFormatter
import com.shopmate.ui.util.labelRes

@Composable
fun SaleRow(
    sale: Sale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemCount = sale.items.sumOf { it.quantity.toInt() }
    val synced = sale.syncedAt != null

    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sale.receiptNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    "${RelativeTimeFormatter.format(sale.createdAt)} · " +
                        "${pluralStringResource(R.plurals.pos_items, itemCount, itemCount)} · " +
                        stringResource(sale.paymentMethod.labelRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                CurrencyFormatter.format(sale.totalAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = if (synced) Icons.Outlined.CloudDone else Icons.Outlined.CloudQueue,
                contentDescription = stringResource(
                    if (synced) R.string.sales_synced else R.string.sales_pending_sync,
                ),
                tint = if (synced) StockGreen else ShopBlackTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
