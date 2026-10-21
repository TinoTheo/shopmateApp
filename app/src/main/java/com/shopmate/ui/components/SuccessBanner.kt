package com.shopmate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.CheckoutNeon
import com.shopmate.ui.theme.CheckoutNeonSubtle
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.Spacing
import com.shopmate.ui.theme.TradeGreen
import kotlinx.coroutines.delay

/** Widest the banner grows — it sits in the content column, never edge-to-edge. */
private val BannerMaxWidth = 480.dp

/**
 * The primary use of Checkout Neon: positive success feedback. Subtle neon-tinted background with
 * a solid neon left accent bar. Slides in from the top and auto-dismisses after 3 seconds.
 * Sized and aligned to the content column — it does not stretch across the screen.
 */
@Composable
fun SuccessBanner(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(3_000)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            color = CheckoutNeonSubtle,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = BannerMaxWidth),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .background(CheckoutNeon),
                )
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = TradeGreen,
                    modifier = Modifier.padding(start = Spacing.md),
                )
                Spacer(Modifier.width(Spacing.md))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                    modifier = Modifier.padding(vertical = Spacing.md, horizontal = Spacing.sm),
                )
            }
        }
    }
}
