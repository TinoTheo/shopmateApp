package com.shopmate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.theme.WarmWhite

private val ButtonShape = RoundedCornerShape(12.dp)
private val ButtonPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp)

/**
 * Buttons size to their label plus balanced padding, then STOP — capped at [ButtonMaxWidth].
 * Nothing stretches: a "Save" button is as wide as "Save" needs, not as wide as the screen.
 * Placement is the caller's job — wrap in a Box/Column/Row with alignment.
 */
val ButtonMinWidth = 120.dp
val ButtonMaxWidth = 280.dp

private fun Modifier.buttonWidth() = widthIn(min = ButtonMinWidth, max = ButtonMaxWidth)

/** Primary call-to-action: Trade Green fill, warm-white text, haptics, optional loading spinner. */
@Composable
fun ShopMatePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.buttonWidth(),
        enabled = enabled && !isLoading,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = TradeGreen,
            contentColor = WarmWhite,
            disabledContainerColor = TradeGreen.copy(alpha = 0.38f),
            disabledContentColor = WarmWhite.copy(alpha = 0.7f),
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = WarmWhite,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text)
        }
    }
}

/** Secondary action: transparent fill, 1.5dp Trade Green border, same sizing discipline. */
@Composable
fun ShopMateSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.buttonWidth(),
        enabled = enabled,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        border = BorderStroke(1.5.dp, SolidColor(TradeGreen)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TradeGreen),
    ) {
        Text(text)
    }
}

/** Lowest-emphasis action: Trade Green text only, no border. Sizes to content. */
@Composable
fun ShopMateTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = TradeGreen),
    ) {
        Text(text)
    }
}
