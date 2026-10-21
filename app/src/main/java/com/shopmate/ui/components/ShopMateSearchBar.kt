package com.shopmate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.InkTertiary
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.theme.WarmBorder
import com.shopmate.ui.theme.WarmWhite

/** Widest the search field grows before it stops — it never spans a whole tablet. */
val SearchBarMaxWidth = 520.dp

/**
 * Search field: warm-white fill with a 1.5dp warm border and a 12dp radius — the
 * "intentional" POS treatment (rectangles aid scanning; heavy rounding hurts it).
 * Icons and hint text use tertiary ink. [trailingContent] hosts fixed-position
 * affordances like the POS barcode trigger.
 */
@Composable
fun ShopMateSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClear: () -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.widthIn(max = SearchBarMaxWidth),
        shape = RoundedCornerShape(12.dp),
        color = WarmWhite,
        border = BorderStroke(1.5.dp, WarmBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = InkTertiary,
                modifier = Modifier.size(20.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.merge(MaterialTheme.typography.bodyLarge).copy(color = Ink),
                    cursorBrush = SolidColor(TradeGreen),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Clear search",
                        tint = InkTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            trailingContent?.invoke()
        }
    }
}
