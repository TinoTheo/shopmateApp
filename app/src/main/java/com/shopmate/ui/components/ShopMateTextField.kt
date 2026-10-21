package com.shopmate.ui.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.InkTertiary
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.theme.WarmBorder
import com.shopmate.ui.theme.WarmWhite

/** Widest a single text field grows — fields are never full-bleed. */
val FieldMaxWidth = 400.dp

/**
 * Standard field: warm-white fill, 1.5dp warm border unfocused, Trade Green focused, red on
 * error, 12dp radius. Capped at [FieldMaxWidth] — a field is a measure to write in, not a
 * band across the screen.
 */
@Composable
fun ShopMateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.widthIn(max = FieldMaxWidth),
        label = { Text(label) },
        shape = RoundedCornerShape(12.dp),
        isError = isError,
        singleLine = singleLine,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage, color = ErrorRed) }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = WarmWhite,
            unfocusedContainerColor = WarmWhite,
            focusedBorderColor = TradeGreen,
            unfocusedBorderColor = WarmBorder,
            errorBorderColor = ErrorRed,
            focusedLabelColor = TradeGreen,
            unfocusedLabelColor = InkTertiary,
            errorLabelColor = ErrorRed,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            cursorColor = TradeGreen,
        ),
    )
}
