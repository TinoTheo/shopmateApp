package com.shopmate.ui.screen.reports

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.PaymentMethod
import com.shopmate.ui.util.CurrencyFormatter
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun PaymentBreakdownBar(
    breakdown: Map<PaymentMethod, BigDecimal>,
    modifier: Modifier = Modifier,
) {
    val total = breakdown.values.fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
    if (breakdown.isEmpty() || total.signum() == 0) return

    // Stable order: highest revenue first.
    val entries = breakdown.entries.sortedByDescending { it.value }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.reports_section_payments),
            style = MaterialTheme.typography.titleMedium,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp)),
        ) {
            entries.forEach { (method, amount) ->
                val fraction = amount.divide(total, 4, RoundingMode.HALF_UP).toFloat()
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction.coerceAtLeast(0.0001f),
                    animationSpec = tween(durationMillis = 600),
                    label = "segment-${method.name}",
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(animatedFraction)
                        .background(paymentColor(method)),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { (method, amount) ->
                val percent = amount.divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100)).toFloat()
                PaymentLegendRow(method = method, amount = amount, percent = percent)
            }
        }
    }
}

@Composable
private fun PaymentLegendRow(method: PaymentMethod, amount: BigDecimal, percent: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(paymentColor(method).copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = paymentIcon(method),
                contentDescription = null,
                tint = paymentIconTint(method),
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = stringResource(paymentLabelRes(method)),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        )
        Text(
            text = CurrencyFormatter.format(amount),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "%.0f%%".format(percent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
