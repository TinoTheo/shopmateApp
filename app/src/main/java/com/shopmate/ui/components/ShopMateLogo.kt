package com.shopmate.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.shopmate.R
import com.shopmate.ui.theme.CheckoutNeon
import com.shopmate.ui.theme.ShopWhite

// The logo art is raster for now (drawable-nodpi PNGs supplied with the brand handoff). The brand
// owner's SVG masters become VectorDrawables under the same resource names, and nothing here
// changes. Do not trace the PNGs by hand.

/** The looped M on its own, in any token colour. Purely decorative unless [contentDescription] is set. */
@Composable
fun ShopMateMark(
    color: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(R.drawable.ic_shopmate_mark),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(color),
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

/**
 * The horizontal wordmark with its leading M in [CheckoutNeon] and the lettering in [ShopWhite],
 * as on the splash and sign-in (handoff 1h, 1i, 1j, 1k). For use on the Trade Green frame only.
 *
 * The art's M ends 53/240 of the way across its canvas (the gap after it runs 20%–24%), so the
 * same image is drawn twice, each clipped to its own side of that line.
 *
 * The tagline "uncompromising control" is set in a serif in the brand art. CLAUDE.md bans
 * serifs and downloadable fonts, so it arrives as part of the lockup vector or not at all —
 * never as UI text.
 */
@Composable
fun ShopMateWordmark(width: Dp, modifier: Modifier = Modifier) {
    val painter = painterResource(R.drawable.ic_shopmate_wordmark)
    val height = width * (painter.intrinsicSize.height / painter.intrinsicSize.width)
    Box(modifier.size(width, height)) {
        WordmarkPart(CheckoutNeon, keepLeftOfSplit = true, width = width, height = height)
        WordmarkPart(ShopWhite, keepLeftOfSplit = false, width = width, height = height)
    }
}

@Composable
private fun WordmarkPart(color: Color, keepLeftOfSplit: Boolean, width: Dp, height: Dp) {
    Image(
        painter = painterResource(R.drawable.ic_shopmate_wordmark),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .size(width, height)
            .drawWithContent {
                val split = size.width * WORDMARK_M_SPLIT
                val keep = if (keepLeftOfSplit) {
                    Rect(0f, 0f, split, size.height)
                } else {
                    Rect(split, 0f, size.width, size.height)
                }
                clipRect(keep.left, keep.top, keep.right, keep.bottom) { this@drawWithContent.drawContent() }
            },
    )
}

private const val WORDMARK_M_SPLIT = 53f / 240f
