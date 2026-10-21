package com.shopmate.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.shopmate.ui.util.CurrencyFormatter
import java.math.BigDecimal

/**
 * A currency value that animates from its previous value to [target] whenever [target] changes.
 * Used for the Revenue Hero Card so the headline number feels alive when the period switches.
 *
 * Set [dimCents] to fade the decimal part — the eye lands on the whole figure first, and the
 * cents stop competing with it. The number is typeset with tabular figures by the caller's style.
 */
@Composable
fun AnimatedCounter(
    target: BigDecimal,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    durationMillis: Int = 800,
    dimCents: Boolean = false,
) {
    val targetFloat = target.toFloat()
    val animated by animateFloatAsState(
        targetValue = targetFloat,
        animationSpec = tween(durationMillis = durationMillis),
        label = "revenue-counter",
    )
    val formatted = CurrencyFormatter.format(BigDecimal.valueOf(animated.toDouble()))
    val text = if (dimCents) {
        val split = formatted.lastIndexOf('.')
        if (split == -1) {
            AnnotatedString(formatted)
        } else {
            buildAnnotatedString {
                append(formatted.substring(0, split))
                withStyle(SpanStyle(color = color.copy(alpha = 0.5f))) {
                    append(formatted.substring(split))
                }
            }
        }
    } else {
        AnnotatedString(formatted)
    }
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = 1,
    )
}
