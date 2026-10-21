package com.shopmate.ui.screen.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.DeltaReason

private val CorrectionBlue = Color(0xFF2196F3)

@Composable
fun QuickActionButtons(
    selectedReason: DeltaReason,
    onSelect: (DeltaReason) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickActionButton(
            label = stringResource(R.string.stock_restock),
            icon = Icons.Outlined.Add,
            color = DeltaPositive,
            selected = selectedReason == DeltaReason.RESTOCK,
            onClick = { onSelect(DeltaReason.RESTOCK) },
            modifier = Modifier.weight(1f),
        )
        QuickActionButton(
            label = stringResource(R.string.stock_adjust),
            icon = Icons.Outlined.Edit,
            color = StockLow,
            selected = selectedReason == DeltaReason.MANUAL_ADJUSTMENT,
            onClick = { onSelect(DeltaReason.MANUAL_ADJUSTMENT) },
            modifier = Modifier.weight(1f),
        )
        QuickActionButton(
            label = stringResource(R.string.stock_correction),
            icon = Icons.Outlined.Build,
            color = CorrectionBlue,
            selected = selectedReason == DeltaReason.CORRECTION,
            onClick = { onSelect(DeltaReason.CORRECTION) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RowScope.QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(68.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = if (selected) {
            BorderStroke(2.dp, color)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) color else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
