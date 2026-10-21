package com.shopmate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.InkSecondary
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.theme.WarmBorder
import com.shopmate.ui.theme.WarmWhite

/**
 * The single chip style used for every filter and period chip.
 * Selected → Trade Green fill, warm-white text. Unselected → transparent fill, warm hairline
 * border, secondary ink text. 10dp corners — crisp, never a full pill (pills read soft and
 * generic). Sizes to its label; never stretches.
 */
@Composable
fun ShopMateFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        label = { Text(if (badge != null) "$label $badge" else label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = InkSecondary,
            selectedContainerColor = TradeGreen,
            selectedLabelColor = WarmWhite,
        ),
        border = if (selected) null else BorderStroke(1.dp, WarmBorder),
    )
}
