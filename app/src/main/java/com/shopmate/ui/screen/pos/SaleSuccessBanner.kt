package com.shopmate.ui.screen.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.ui.theme.CheckoutNeon
import com.shopmate.ui.theme.CheckoutNeonSubtle
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.InkSecondary
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun SaleSuccessBanner(
    receipt: String?,
    modifier: Modifier = Modifier,
    total: BigDecimal? = null,
) {
    // Keep the last receipt/total so the text stays stable while the exit animation plays.
    var lastReceipt by remember { mutableStateOf("") }
    var lastTotal by remember { mutableStateOf("") }
    if (receipt != null) lastReceipt = receipt
    if (total != null) lastTotal = CurrencyFormatter.format(total)

    AnimatedVisibility(
        visible = receipt != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        // Two-colour discipline: neon only as the 4dp left accent, subtle neon tint as background.
        // Sized to the content column — a success note, not a banner across the whole screen.
        Surface(
            color = CheckoutNeonSubtle,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 480.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(56.dp)
                        .background(CheckoutNeon),
                )
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = TradeGreen,
                    modifier = Modifier.padding(start = 16.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        stringResource(R.string.pos_sale_complete, lastTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                    )
                    Text(
                        stringResource(R.string.pos_receipt, lastReceipt),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                    )
                }
            }
        }
    }
}
