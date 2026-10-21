package com.shopmate.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// ShopMate is light-only — POS systems run in well-lit shops. No dark scheme, no dynamic colour.
// Every neutral is warm-tinted; nothing is pure white or pure black.
//
// Where a dark scheme would need decisions, when it comes: CheckoutNeon relies on Ink-on-neon
// for contrast and would need re-tuning; the sidebar is already dark, so an inverted app loses
// the frame/sheet distinction that carries the whole layout; and StockOrange vs TrendDown would
// need re-separating at low luminance.
private val WarmLightScheme = lightColorScheme(
    primary = TradeGreen,
    onPrimary = WarmWhite,
    primaryContainer = TradeGreen,
    onPrimaryContainer = WarmWhite,

    secondary = WarmSand,
    onSecondary = Ink,
    secondaryContainer = WarmSand,
    onSecondaryContainer = Ink,

    tertiary = WarmGold,
    onTertiary = WarmWhite,
    tertiaryContainer = WarmGoldSubtle,
    onTertiaryContainer = Ink,

    background = WarmWhite,
    onBackground = Ink,
    surface = WarmWhite,
    onSurface = Ink,
    surfaceVariant = WarmSurface,
    onSurfaceVariant = InkSecondary,
    // Keep the container ramp on the warm whites so any default M3 card/surface stays warm
    // rather than picking up a neutral grey — surfaces must stay flat and warm.
    surfaceContainerLowest = WarmWhite,
    surfaceContainerLow = WarmWhite,
    surfaceContainer = WarmSurface,
    surfaceContainerHigh = WarmSurface,
    surfaceContainerHighest = WarmSurface,

    outline = WarmBorder,
    outlineVariant = WarmSand,

    error = ErrorRed,
    onError = WarmWhite,
    errorContainer = WarmGoldSubtle,
    onErrorContainer = ErrorRed,

    inverseSurface = Ink,
    inverseOnSurface = WarmWhite,
    inversePrimary = CheckoutNeon,
)

// Crisp corners — enough to feel considered, never so round it reads soft/generic.
private val ShopMateShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun ShopMateTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = WarmLightScheme,
        typography = ShopMateTypography,
        shapes = ShopMateShapes,
        content = content,
    )
}
