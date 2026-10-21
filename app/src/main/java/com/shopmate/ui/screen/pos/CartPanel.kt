package com.shopmate.ui.screen.pos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.CartItem
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopOffWhite
import com.shopmate.ui.util.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun CartPanel(
    cartItems: List<CartItem>,
    cartTotal: BigDecimal,
    cartItemCount: Int,
    highlightedProductId: String?,
    onIncrement: (CartItem) -> Unit,
    onDecrement: (CartItem) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
    // Wide-layout mode: the panel fills the height and the item list expands,
    // instead of the phone's bottom-docked panel with a capped list.
    sidePanel: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ShopOffWhite,
        shadowElevation = if (sidePanel) 0.dp else 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (sidePanel) Modifier.fillMaxHeight() else Modifier)
                .padding(top = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.pos_cart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (cartItemCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(10.dp), color = ReceiptBeige) {
                        Text(
                            cartItemCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = ShopBlack,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (cartItems.isNotEmpty()) {
                    IconButton(onClick = onClearCart) {
                        Icon(
                            Icons.Outlined.RemoveShoppingCart,
                            contentDescription = stringResource(R.string.pos_clear_cart),
                        )
                    }
                }
            }

            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (sidePanel) Modifier.weight(1f) else Modifier)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.pos_cart_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (sidePanel) Modifier.weight(1f) else Modifier.heightIn(max = 240.dp)),
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        CartItemRow(
                            item = item,
                            highlighted = item.product.id == highlightedProductId,
                            onIncrement = { onIncrement(item) },
                            onDecrement = { onDecrement(item) },
                        )
                        HorizontalDivider()
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.pos_total),
                    style = MaterialTheme.typography.titleMedium,
                    color = ShopBlackSecondary,
                )
                Spacer(Modifier.weight(1f))
                AnimatedContent(
                    targetState = cartTotal,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "cartTotal",
                ) { total ->
                    Text(
                        CurrencyFormatter.format(total),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ShopBlack,
                    )
                }
            }

            ChargeButton(
                total = cartTotal,
                enabled = cartItems.isNotEmpty(),
                onClick = onCheckout,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
