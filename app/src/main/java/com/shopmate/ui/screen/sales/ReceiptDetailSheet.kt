package com.shopmate.ui.screen.sales

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.Sale
import com.shopmate.domain.model.SaleItem
import com.shopmate.ui.components.ShopMateSecondaryButton
import com.shopmate.ui.receipt.ReceiptContent
import com.shopmate.ui.receipt.ReceiptShare
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.StockGreen
import com.shopmate.ui.util.CurrencyFormatter
import com.shopmate.ui.util.labelRes
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val receiptDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailSheet(
    sale: Sale,
    storeName: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                sale.receiptNumber,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                receiptDateFormat.format(sale.createdAt.atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = ReceiptBeige)
            Spacer(Modifier.height(12.dp))

            sale.items.forEach { item ->
                ReceiptItemRow(item)
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(2.dp))
            HorizontalDivider(color = ReceiptBeige)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.pos_total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    CurrencyFormatter.format(sale.totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.receipt_paid_via, stringResource(sale.paymentMethod.labelRes())),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.receipt_cashier, sale.cashierName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(
                        if (sale.syncedAt != null) R.string.sales_synced else R.string.sales_pending_sync,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (sale.syncedAt != null) {
                        StockGreen
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Spacer(Modifier.height(20.dp))
            // The customer's copy: the 1m image, sent through whatever app they use.
            ShopMateSecondaryButton(
                text = stringResource(R.string.receipt_share),
                onClick = {
                    scope.launch {
                        ReceiptShare.share(
                            context = context,
                            receipt = ReceiptContent.from(context, sale, storeName),
                            receiptNumber = sale.receiptNumber,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ReceiptItemRow(item: SaleItem) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.productName,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(
                    R.string.receipt_qty_at_price,
                    item.quantity.stripTrailingZeros().toPlainString(),
                    CurrencyFormatter.format(item.unitPrice),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            CurrencyFormatter.format(item.lineTotal),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}
