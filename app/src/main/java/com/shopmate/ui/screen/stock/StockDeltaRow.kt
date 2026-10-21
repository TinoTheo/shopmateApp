package com.shopmate.ui.screen.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.DeltaReason
import com.shopmate.domain.model.StockDelta
import com.shopmate.ui.theme.StockOrange
import com.shopmate.ui.theme.ShopMateTheme
import com.shopmate.ui.util.RelativeTimeFormatter
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun StockDeltaRow(
    delta: StockDelta,
    modifier: Modifier = Modifier,
    productName: String? = null,
) {
    val deltaColor = if (delta.delta >= 0) DeltaPositive else DeltaNegative
    val deltaText = if (delta.delta >= 0) "+${delta.delta}" else delta.delta.toString()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            deltaText,
            modifier = Modifier.width(52.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = deltaColor,
            textAlign = TextAlign.Start,
        )

        Column(modifier = Modifier.weight(1f)) {
            if (productName != null) {
                Text(
                    productName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReasonChip(delta.reason)
                if (!delta.note.isNullOrBlank()) {
                    Text(
                        delta.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))
        Text(
            RelativeTimeFormatter.format(delta.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReasonChip(reason: DeltaReason) {
    val (labelRes, tint) = when (reason) {
        DeltaReason.SALE -> R.string.reason_sale to DeltaNegative
        DeltaReason.RESTOCK -> R.string.reason_restock to DeltaPositive
        DeltaReason.MANUAL_ADJUSTMENT -> R.string.reason_adjustment to StockLow
        DeltaReason.CORRECTION -> R.string.reason_correction to androidx.compose.ui.graphics.Color(0xFF2196F3)
        DeltaReason.PURCHASE -> R.string.reason_purchase to DeltaPositive
        // Count corrections read as Warning, not as a plain adjustment (CLAUDE.md § Severity).
        DeltaReason.COUNT -> R.string.reason_count to StockOrange
    }
    Surface(
        color = tint.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            stringResource(labelRes),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StockDeltaRowPreview() {
    ShopMateTheme {
        Column {
            StockDeltaRow(
                delta = StockDelta("1", "p1", 20, DeltaReason.RESTOCK, "Supplier delivery", createdAt = Instant.now().minus(2, ChronoUnit.HOURS)),
                productName = "Coca Cola 500ml",
            )
            StockDeltaRow(
                delta = StockDelta("2", "p2", -3, DeltaReason.SALE, "SM-20260630-0001", createdAt = Instant.now().minus(1, ChronoUnit.DAYS)),
                productName = "Bread Loaf",
            )
        }
    }
}
