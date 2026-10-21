package com.shopmate.ui.screen.pos

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.R
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.ShopWhite

/**
 * The always-visible POS keypad: quantity entry, manual price, cash-tendered change.
 * Stateless — every mode/buffer decision lives in [PosViewModel]; keys just report presses.
 */
@Composable
fun NumberPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { digit ->
                    DigitKey(
                        label = digit,
                        onPress = { onDigit(digit) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            DigitKey(
                label = ".",
                muted = true,
                onPress = { onDigit(".") },
                modifier = Modifier.weight(1f),
            )
            DigitKey(
                label = "0",
                onPress = { onDigit("0") },
                modifier = Modifier.weight(1f),
            )
            PadKey(onPress = onBackspace, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = stringResource(R.string.pos_backspace),
                    tint = ShopBlackTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun DigitKey(
    label: String,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    PadKey(onPress = onPress, modifier = modifier) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (muted) ShopBlackTertiary else ShopBlack,
        )
    }
}

@Composable
private fun PadKey(
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        if (pressed) ReceiptBeige else ShopWhite,
        label = "padKeyPress",
    )

    Surface(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onPress()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(9.dp),
        color = background,
        border = BorderStroke(1.dp, ReceiptBeige),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.padding(vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
