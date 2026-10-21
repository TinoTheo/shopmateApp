package com.shopmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small circle in a category's own colour — the only place arbitrary colours appear in the app.
 * If [colour] can't be parsed, nothing is drawn.
 */
@Composable
fun CategoryDot(
    colour: String?,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
) {
    val parsed = parseHexColour(colour) ?: return
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(parsed),
    )
}

private fun parseHexColour(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
}
