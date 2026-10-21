package com.shopmate.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.WarmSand

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
    // Warm sand shimmering against itself at 40% — never a cold grey pulse.
    return Brush.linearGradient(
        colors = listOf(WarmSand, WarmSand.copy(alpha = 0.4f), WarmSand),
        start = Offset(translate - 300f, 0f),
        end = Offset(translate, 0f),
    )
}

/** A shimmering placeholder box. Give it a size + [corner] to match the component it stands in for. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, corner: Int = 8) {
    Spacer(
        modifier = modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(shimmerBrush()),
    )
}

/** Card-shaped shimmer matching ShopMateCard's radius. */
@Composable
fun ShimmerCard(modifier: Modifier = Modifier, height: Int = 96) {
    ShimmerBox(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        corner = 12,
    )
}

/** Staggered text-line shimmer: widths step down 100% / 80% / 60% for realism. */
@Composable
fun ShimmerText(modifier: Modifier = Modifier, lines: Int = 3) {
    val fractions = listOf(1f, 0.8f, 0.6f)
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(lines) { index ->
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(fractions[index % fractions.size])
                    .height(14.dp),
                corner = 6,
            )
            if (index != lines - 1) Spacer(Modifier.height(8.dp))
        }
    }
}
