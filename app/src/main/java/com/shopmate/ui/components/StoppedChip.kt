package com.shopmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopMateTheme
import com.shopmate.ui.theme.ShopWhite

/**
 * The **Stopped** severity tier: already costing money — out of stock, or selling at a loss
 * (CLAUDE.md § Severity).
 *
 * A [ShopBlack] chip with white text, deliberately **not** red. An empty shelf is not an error;
 * it is a fact that costs money, and it reads as stopped rather than broken. `ErrorRed` is for
 * failures only.
 */
@Composable
fun StoppedChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(20.dp)
            .background(ShopBlack, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = ShopWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFCFA)
@Composable
private fun StoppedChipPreview() {
    ShopMateTheme {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            StoppedChip("Out")
            StoppedChip("At a loss")
        }
    }
}
