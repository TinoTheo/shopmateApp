package com.shopmate.ui.screen.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.domain.model.TopProduct
import java.math.BigDecimal

@Composable
fun TopProductsList(
    products: List<TopProduct>,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.reports_section_best),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (products.isEmpty()) {
            EmptyTopProducts(emptyText)
            return@Column
        }

        val topRevenue = products.first().revenue.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
        products.forEachIndexed { index, product ->
            val fraction = product.revenue.divide(topRevenue, 4, java.math.RoundingMode.HALF_UP).toFloat()
            StaggeredItem(index = index) {
                TopProductRow(rank = index + 1, product = product, fillFraction = fraction)
            }
        }
    }
}

@Composable
private fun StaggeredItem(index: Int, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = index * 60)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 300, delayMillis = index * 60, easing = LinearOutSlowInEasing),
                initialOffsetY = { it / 4 },
            ),
    ) {
        content()
    }
}

@Composable
private fun EmptyTopProducts(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.Insights,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
