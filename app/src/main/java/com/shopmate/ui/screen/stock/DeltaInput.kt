package com.shopmate.ui.screen.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.shopmate.R

@Composable
fun DeltaInput(
    isPositive: Boolean,
    magnitude: String,
    onMagnitudeChange: (String) -> Unit,
    onToggleSign: () -> Unit,
    newStock: Int,
    modifier: Modifier = Modifier,
) {
    val signColor = if (isPositive) DeltaPositive else DeltaNegative

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalIconButton(
                onClick = onToggleSign,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = signColor.copy(alpha = 0.15f),
                    contentColor = signColor,
                ),
            ) {
                Icon(
                    if (isPositive) Icons.Outlined.Add else Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.stock_toggle_sign),
                )
            }

            OutlinedTextField(
                value = magnitude,
                onValueChange = { input -> onMagnitudeChange(input.filter { it.isDigit() }.take(6)) },
                modifier = Modifier.width(150.dp),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    color = signColor,
                    fontWeight = FontWeight.SemiBold,
                ),
                placeholder = {
                    Text(
                        "0",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }

        Spacer(Modifier.height(12.dp))

        if (newStock < 0) {
            Text(
                stringResource(R.string.stock_cannot_negative),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = DeltaNegative,
            )
        } else {
            Text(
                stringResource(R.string.stock_new_will_be, newStock),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
