package com.shopmate.ui.screen.pos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.Product
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.components.StoppedChip
import com.shopmate.ui.theme.StockOrange
import com.shopmate.ui.theme.TrendUp
import com.shopmate.ui.util.CurrencyFormatter

@Composable
fun ProductGridCard(
    product: Product,
    categoryColour: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "cardPress")

    // An empty shelf reads as Stopped, not broken: a ShopBlack chip, never StockRed (see
    // Severity in CLAUDE.md). ErrorRed and StockRed are for failures.
    val outOfStock = product.stockQuantity <= 0
    val stockColor = if (product.stockQuantity <= product.lowStockThreshold) StockOrange else TrendUp
    val stockLabel = "${product.stockQuantity} ${product.unit}"
    val dotColor = categoryColour?.let {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
    }

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(116.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ShopWhite),
        border = BorderStroke(1.dp, ReceiptBeige),
        // Flat by design — press feedback comes from the scale animation, not a shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 14.dp),
                )
                Column {
                    Text(
                        CurrencyFormatter.format(product.sellingPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = ShopBlack,
                    )
                    if (outOfStock) {
                        StoppedChip(stringResource(R.string.stock_stopped_out))
                    } else {
                        Text(
                            stockLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = stockColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
