package com.shopmate.ui.screen.pos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.CartItem
import com.shopmate.ui.components.QuantityControl
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.CurrencyFormatter

@Composable
fun CartItemRow(
    item: CartItem,
    highlighted: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Transient "just added" flash — a faint Trade Green tint (never neon on a surface).
    val highlightColor by animateColorAsState(
        targetValue = if (highlighted) TradeGreen.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = tween(durationMillis = 600),
        label = "cartHighlight",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(highlightColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = ShopBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // Line math shown ("2 × $1.50") — reduces total disputes; the cashier sees the
            // breakdown without reopening the line.
            Text(
                stringResource(
                    R.string.receipt_qty_at_price,
                    item.quantity.toString(),
                    CurrencyFormatter.format(item.unitPrice),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        QuantityControl(
            quantity = item.quantity,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
        )

        Spacer(Modifier.width(12.dp))
        Text(
            CurrencyFormatter.format(item.lineTotal),
            modifier = Modifier.widthIn(min = 64.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = ShopBlack,
        )
    }
}
