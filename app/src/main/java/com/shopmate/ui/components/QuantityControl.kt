package com.shopmate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.TradeGreen

/** Minus / quantity / plus control. Circles are outlined Trade Green; the number is black. */
@Composable
fun QuantityControl(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        QtyCircle(symbol = "−") {
            if (quantity <= 1) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onDecrement()
        }
        Text(
            text = "$quantity",
            color = ShopBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        QtyCircle(symbol = "+", onClick = onIncrement)
    }
}

@Composable
private fun QtyCircle(symbol: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, TradeGreen),
        modifier = Modifier.size(22.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = symbol, color = TradeGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
