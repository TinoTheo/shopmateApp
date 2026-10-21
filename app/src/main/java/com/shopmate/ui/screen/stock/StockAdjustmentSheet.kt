package com.shopmate.ui.screen.stock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.DeltaReason
import com.shopmate.domain.model.Product
import com.shopmate.domain.model.StockDelta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentSheet(
    product: Product,
    history: List<StockDelta>,
    onConfirm: (delta: Int, reason: DeltaReason, note: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var reason by remember { mutableStateOf(DeltaReason.RESTOCK) }
    var isPositive by remember { mutableStateOf(true) }
    var magnitude by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val signedDelta = (magnitude.toIntOrNull() ?: 0).let { if (isPositive) it else -it }
    val newStock = product.stockQuantity + signedDelta
    val canConfirm = signedDelta != 0 && newStock >= 0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                product.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    R.string.stock_current,
                    "${product.stockQuantity} ${product.unit}",
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = stockColorFor(product),
            )
            Text(
                stringResource(R.string.stock_alert_below, product.lowStockThreshold),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            QuickActionButtons(
                selectedReason = reason,
                onSelect = { selected ->
                    reason = selected
                    // Restock implies adding stock; pre-select the positive sign.
                    if (selected == DeltaReason.RESTOCK) isPositive = true
                },
            )

            Spacer(Modifier.height(20.dp))
            DeltaInput(
                isPositive = isPositive,
                magnitude = magnitude,
                onMagnitudeChange = { magnitude = it },
                onToggleSign = { isPositive = !isPositive },
                newStock = newStock,
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.stock_note_placeholder)) },
                supportingText = { Text(stringResource(R.string.stock_note_helper)) },
                singleLine = true,
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onConfirm(signedDelta, reason, note.ifBlank { null }) },
                enabled = canConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.shopmate.ui.theme.TradeGreen,
                ),
            ) {
                Text(
                    stringResource(R.string.stock_update_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(20.dp))
            StockHistoryList(history = history)
        }
    }
}
