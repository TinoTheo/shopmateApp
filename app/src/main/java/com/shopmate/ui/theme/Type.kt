package com.shopmate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shopmate.R

// One family, three weights, total consistency. Inter — a geometric sans-serif designed for
// screens — for the entire app. No secondary display font. Never Bold (700); the jump from
// Medium to SemiBold carries all the hierarchy we need.
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

// === NUMBER STYLES ===
// Every price, total, revenue figure, stock count, and metric uses these: tabular ("tnum")
// figures so digits share one width and columns align down a list. Prose uses regular Inter.
val NumberLarge = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.6).sp,
    fontFeatureSettings = "tnum",
)
val NumberMedium = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.4).sp,
    fontFeatureSettings = "tnum",
)
val NumberSmall = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum",
)

/**
 * Eyebrow label — the section headers on Stock and Products (CLAUDE.md § Typography).
 *
 * Callers pass already-uppercased text; casing is content, not styling, so it does not belong
 * in a `Modifier` stack at the call site. Carries its own colour ([InkMid]) so a section header
 * cannot drift between screens.
 */
val Eyebrow = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Medium,
    fontSize = 10.5.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.155.sp, // 0.11em
    color = InkMid,
)

// Display / headline / title sizes pull in slightly (negative tracking) so big text reads
// typeset rather than dumped. Body and label sizes keep their spacing — small text needs it.
val ShopMateTypography = Typography(
    // Display — hero numbers (Revenue Hero Card total)
    displayLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.4).sp,
    ),

    // Headline — section headers
    headlineLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),

    // Title — app bar, card titles
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),

    // Body — product names, cart items, descriptions
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),

    // Label — chips, badges, nav labels, status text
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
    ),
)

// === ROW-SCALE NUMBER STYLES ===
// InsightRow and LedgerRow sit at sizes the three styles above do not cover. They live here, not
// at the call sites, so a total counting up never shifts the layout beside it (CLAUDE.md § Numbers).

/** InsightRow trailing value — 14.5sp SemiBold. */
val NumberRowValue = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.5.sp,
    lineHeight = 19.sp,
    fontFeatureSettings = "tnum",
)

/** InsightRow trailing sub-line — 11sp. */
val NumberRowSub = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    fontFeatureSettings = "tnum",
)

/** LedgerRow signed delta — 13sp SemiBold. */
val NumberLedgerDelta = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontFeatureSettings = "tnum",
)

/** LedgerRow running balance — 12sp. */
val NumberLedgerBalance = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontFeatureSettings = "tnum",
)
