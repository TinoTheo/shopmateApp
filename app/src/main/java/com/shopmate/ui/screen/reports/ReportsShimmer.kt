package com.shopmate.ui.screen.reports

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-translate",
    )
    val colors = listOf(
        Color.LightGray.copy(alpha = 0.35f),
        Color.LightGray.copy(alpha = 0.15f),
        Color.LightGray.copy(alpha = 0.35f),
    )
    return Brush.linearGradient(
        colors = colors,
        start = Offset(translate - 300f, 0f),
        end = Offset(translate, 0f),
    )
}

@Composable
private fun ShimmerBlock(height: Int, modifier: Modifier = Modifier, corner: Int = 12) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(corner.dp))
            .background(shimmerBrush()),
    )
}

/** Loading skeleton matching the Reports layout: hero card, chart, and a few list rows. */
@Composable
fun ReportsShimmer(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ShimmerBlock(height = 150, corner = 16)   // hero card
        ShimmerBlock(height = 220, corner = 16)   // chart
        ShimmerBlock(height = 18)                 // payment bar
        repeat(4) { ShimmerBlock(height = 44) }   // top product rows
    }
}
