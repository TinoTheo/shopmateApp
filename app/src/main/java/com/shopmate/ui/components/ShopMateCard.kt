package com.shopmate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.Spacing
import com.shopmate.ui.theme.WarmBorder
import com.shopmate.ui.theme.WarmSurface
import com.shopmate.ui.theme.WarmWhite

private val CardShape = RoundedCornerShape(14.dp)

/** Widest a standalone card grows before it stops — cards never sprawl edge-to-edge on tablets. */
val CardMaxWidth = 600.dp

/**
 * The one and only card style: warm-white fill, 1dp warm hairline border, flat (NO elevation,
 * no shadow), 14dp radius, 16dp inner padding. Cards in a grid size to their cell; standalone
 * cards sit within generous margins and never stretch full-bleed.
 *
 * Set [nested] for a card sitting on top of another warm-white surface — it picks up the
 * slightly deeper [WarmSurface] fill so the edge still reads without a shadow.
 */
@Composable
fun ShopMateCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    nested: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = if (nested) WarmSurface else WarmWhite,
        contentColor = Ink,
    )
    val border = BorderStroke(1.dp, WarmBorder)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    // The cap lives here, not at the call sites: a caller may still say fillMaxWidth() for a
    // list row, but no card can sprawl past [CardMaxWidth] on a wide screen.
    val sized = modifier.widthIn(max = CardMaxWidth)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = sized,
            shape = CardShape,
            colors = colors,
            border = border,
            elevation = elevation,
        ) {
            Column(modifier = Modifier.padding(Spacing.lg), content = content)
        }
    } else {
        Card(
            modifier = sized,
            shape = CardShape,
            colors = colors,
            border = border,
            elevation = elevation,
        ) {
            Column(modifier = Modifier.padding(Spacing.lg), content = content)
        }
    }
}
