package com.shopmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.InkSecondary
import com.shopmate.ui.theme.Spacing
import com.shopmate.ui.theme.WarmGold
import com.shopmate.ui.theme.WarmSand
import com.shopmate.ui.theme.WarmSurface

/**
 * A warm high note: top seller, a daily milestone, a greeting row. Gold appears HERE and
 * essentially nowhere else — it earns its meaning by being rare.
 *
 * Flat warm surface, icon in a sand well, sized to content (capped) and never stretched.
 */
@Composable
fun GoldMoment(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 480.dp),
        shape = RoundedCornerShape(12.dp),
        color = WarmSurface,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(WarmSand),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = WarmGold,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.padding(start = Spacing.md)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                )
            }
        }
    }
}
