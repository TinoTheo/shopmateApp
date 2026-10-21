package com.shopmate.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.Eyebrow
import com.shopmate.ui.theme.ShopMateTheme
import com.shopmate.ui.theme.ShopBlackMid
import com.shopmate.ui.theme.TradeGreen

/**
 * The eyebrow label that opens each section on Stock and Products, with the count of what is
 * in it and an optional trailing action.
 *
 * The label style is [Eyebrow] from `Type.kt` — a named style, never an inline modifier stack
 * (CLAUDE.md § Typography).
 */
@Composable
fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = (if (count != null) "$label · $count" else label).uppercase(),
            style = Eyebrow,
            color = ShopBlackMid,
        )
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = Eyebrow,
                color = TradeGreen,
                modifier = Modifier.clickable(onClick = onActionClick),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFCFA)
@Composable
private fun SectionHeaderPreview() {
    ShopMateTheme {
        androidx.compose.foundation.layout.Column {
            SectionHeader(label = "Runs out this week", count = 6)
            SectionHeader(label = "Stopped", count = 2, actionLabel = "Order list", onActionClick = {})
        }
    }
}
