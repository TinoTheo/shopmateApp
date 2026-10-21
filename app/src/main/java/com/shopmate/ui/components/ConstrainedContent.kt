package com.shopmate.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Widest any list / card column may grow on a tablet. */
val ContentMaxWidth = 760.dp

/** Widest a single-column form (text fields) may grow. */
val FormMaxWidth = 560.dp

/**
 * Hard width cap for screen content: fields, cards, and list rows inside never
 * stretch across the whole sheet on wide screens — they grow to [maxWidth] and
 * center. On phones the cap is wider than the screen, so this is a no-op.
 */
@Composable
fun ConstrainedContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ContentMaxWidth,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
            content = content,
        )
    }
}
