package com.shopmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.InkSecondary
import com.shopmate.ui.theme.Spacing
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.theme.WarmGold
import com.shopmate.ui.theme.WarmSand

/**
 * Shared empty state: the icon sits in a warm sand well rather than floating, so an empty
 * screen still looks composed. [positive] tints the icon warm gold (a good-news empty, like
 * "nothing's low on stock"); otherwise it's a quiet Trade Green.
 *
 * Centered with generous vertical room, capped in width so the copy keeps a readable measure,
 * and any action is a sized button — nothing stretches.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    positive: Boolean = false,
) {
    Column(
        modifier = modifier
            .widthIn(max = 360.dp)
            .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WarmSand),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (positive) WarmGold else TradeGreen.copy(alpha = 0.4f),
                modifier = Modifier.size(44.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onAction != null) {
            ShopMatePrimaryButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.padding(top = Spacing.md),
            )
        }
    }
}
