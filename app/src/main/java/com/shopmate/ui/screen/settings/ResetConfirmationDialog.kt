package com.shopmate.ui.screen.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.shopmate.R
import com.shopmate.ui.components.ShopMateTextField
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.TradeGreen

private const val CONFIRM_WORD = "DELETE"

/**
 * Two-stage guard for the nuclear reset: a plain warning, then a "type DELETE" confirmation.
 * [onConfirmed] fires only after the exact word is typed.
 */
@Composable
fun ResetConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
) {
    var stage by remember { mutableStateOf(1) }
    var typed by remember { mutableStateOf("") }

    when (stage) {
        1 -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_reset)) },
            text = { Text(stringResource(R.string.settings_reset_warning)) },
            confirmButton = {
                TextButton(onClick = { stage = 2 }) {
                    Text(stringResource(R.string.action_confirm), color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = ShopBlackTertiary) }
            },
        )
        else -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_reset_confirm_type)) },
            text = {
                ShopMateTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = CONFIRM_WORD,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmed,
                    enabled = typed == CONFIRM_WORD,
                ) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = if (typed == CONFIRM_WORD) ErrorRed else ShopBlackTertiary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = TradeGreen) }
            },
        )
    }
}
