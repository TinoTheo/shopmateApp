package com.shopmate.ui.screen.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shopmate.domain.model.TopProduct
import com.shopmate.ui.theme.RankGold
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.CurrencyFormatter

/**
 * A single ranked product. [fillFraction] (0..1) drives the subtle revenue-dominance bar behind
 * the row — 1f for the top seller, proportional for the rest.
 */
@Composable
fun TopProductRow(
    rank: Int,
    product: TopProduct,
    fillFraction: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
    ) {
        // Revenue-dominance fill behind the content — faint, heavier for the top seller.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fillFraction.coerceIn(0f, 1f))
                .background(TradeGreen.copy(alpha = if (rank == 1) 0.06f else 0.04f)),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankBadge(rank)
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = product.productName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (product.categoryName != null) {
                    CategoryChip(name = product.categoryName, colourHex = product.categoryColour)
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = CurrencyFormatter.format(product.revenue),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${product.quantitySold} sold",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    // Two-colour discipline: no medal chips — just a coloured number. Gold for #1, tertiary rest.
    Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (rank == 1) FontWeight.SemiBold else FontWeight.Medium,
            color = if (rank == 1) RankGold else ShopBlackTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CategoryChip(name: String, colourHex: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(parseHexColour(colourHex) ?: MaterialTheme.colorScheme.outline),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun parseHexColour(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
}
