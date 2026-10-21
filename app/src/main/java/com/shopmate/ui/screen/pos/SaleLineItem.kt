package com.shopmate.ui.screen.pos

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.R
import com.shopmate.domain.model.CartItem
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.CurrencyFormatter

/**
 * One line on the sale ticket: name + "qty × price" breakdown on the left, line total on
 * the right. Tap selects it for quantity editing on the number pad; long-press switches to
 * manual price entry. The visible line math ("2 × $1.50") is deliberate — it reduces total
 * disputes because the cashier sees the breakdown without reopening the line.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SaleLineItem(
    item: CartItem,
    selected: Boolean,
    editingQty: String?,    // non-null while the pad is typing this line's quantity
    editingPrice: String?,  // non-null while the pad is typing this line's price
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        if (selected) ReceiptBeige.copy(alpha = 0.45f) else Color.Transparent,
        label = "lineSelected",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = item.product.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ShopBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            QtyBreakdown(item = item, editingQty = editingQty, editingPrice = editingPrice)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = CurrencyFormatter.format(item.lineTotal),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ShopBlack,
        )
    }
}

@Composable
private fun QtyBreakdown(item: CartItem, editingQty: String?, editingPrice: String?) {
    val qtyText = when {
        editingQty != null -> editingQty.ifEmpty { "…" }
        else -> item.quantity.toString()
    }
    val priceText = when {
        editingPrice != null -> editingPrice.ifEmpty { "…" }
        else -> CurrencyFormatter.format(item.unitPrice)
    }
    Row {
        Text(
            text = qtyText,
            fontSize = 11.sp,
            fontWeight = if (editingQty != null) FontWeight.Bold else FontWeight.Normal,
            color = if (editingQty != null) TradeGreen else ShopBlackTertiary,
        )
        Text(
            text = " ${stringResource(R.string.pos_times)} ",
            fontSize = 11.sp,
            color = ShopBlackTertiary,
        )
        Text(
            text = priceText,
            fontSize = 11.sp,
            fontWeight = if (editingPrice != null) FontWeight.Bold else FontWeight.Normal,
            color = if (editingPrice != null) TradeGreen else ShopBlackTertiary,
        )
    }
}
