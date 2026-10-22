package com.shopmate.ui.theme

import androidx.compose.ui.graphics.Color

// ShopMate is WARM & CRISP: warm in temperature, voice, and space; crisp in structure —
// flat surfaces, clean edges, no ornament, no shadows.
//
// The two brand colours stay. Every neutral is warm-tinted: pure white (#FFFFFF) and pure
// black (#000000) are RETIRED from the UI. One warm gold accent marks human high notes only.
// If a colour isn't defined here, it doesn't exist in ShopMate.

// === THE TWO BRAND COLOURS (unchanged) ===
val TradeGreen = Color(0xFF005B52)

/**
 * Splash only. The oversized looped M that bleeds off the phone splash (brand handoff 1h) is
 * drawn in this deeper green on [TradeGreen] — the same as [Ink] at 18% over Trade Green.
 * It is not a third brand colour: nothing else in the app uses it.
 */
val TradeGreenDeep = Color(0xFF014A43)

/**
 * Checkout Neon has exactly two jobs, in different parts of the screen (CLAUDE.md § The
 * Checkout Neon rule):
 *
 * **In the frame** — permanent identity: the brand mark and the selected sidebar pill. These
 * are always present and do not respond to what the user just did.
 *
 * **In the content sheet** — a commit that succeeded and cannot be undone, and nothing else.
 * At most one or two per screen: POS the Charge button, the queue tick as it uploads and the
 * confirmation banner edge; Stock `Commit count`; Products `Save new price`; Reports none.
 *
 * If a new element wants Neon and it is neither the frame nor a commit, it does not get it.
 * Selection states, hover, focus, trend arrows and "good" values are **not** commits — trend-up
 * uses [TrendUp], focus rings use [TradeGreen] ([CheckoutNeon] on the green frame).
 */
val CheckoutNeon = Color(0xFF99FFAA)
val CheckoutNeonSubtle = Color(0xFFE8FFF0)

// === WARM NEUTRALS (replaces cold greys) ===
val WarmWhite = Color(0xFFFDFCFA)        // main background — never pure white
val WarmSurface = Color(0xFFFBF9F6)      // raised / nested surfaces
val WarmSand = Color(0xFFF0EDE7)         // borders-as-fill, chips, wells
val WarmBorder = Color(0xFFECE7E0)       // hairline borders
val WarmBorderStrong = Color(0xFFDDD6CC) // stronger dividers

// === WARM INK (text) ===
val Ink = Color(0xFF1A1A1A)              // primary text — never pure black
val InkSecondary = Color(0xFF6B6459)     // secondary — warm grey-brown

/**
 * Mid ink tier — dormant emphasis (CLAUDE.md § Severity) and eyebrow labels
 * (§ Typography). Sits between [InkSecondary] and [InkTertiary]: quieter than secondary body
 * copy, but still legible as content rather than reading as a disabled hint.
 *
 * Added in v3.0 as a deliberate exception to "no new colours" — § Severity and § Typography
 * both require a tier that did not exist, and reusing [InkTertiary] would have made dormant
 * rows read as placeholder text.
 */
val InkMid = Color(0xFF8A8175)

val InkTertiary = Color(0xFFA89F94)      // hints, placeholders

// === WARM GOLD ACCENT (human high notes only: top seller, milestones, greetings) ===
val WarmGold = Color(0xFFC9A227)
val WarmGoldSubtle = Color(0xFFF7F0DC)

// === SEMANTIC (functional, never decorative — warmed) ===
// Severity (CLAUDE.md § Severity) maps onto these: Warning is StockOrange + its tint surface;
// Stopped is a ShopBlack chip with white text. ErrorRed is for FAILURES, never for business
// conditions — an empty shelf is stopped, not broken.
val StockGreen = Color(0xFF2E7D32)
val StockOrange = Color(0xFFE07B39)
val StockOrangeSubtle = Color(0xFFFBF0E7) // warning tint surface (variance rows, strips)
val StockRed = Color(0xFFCC4436)
val ErrorRed = Color(0xFFCC4436)
val TrendUp = Color(0xFF2E7D32)          // NOT CheckoutNeon — v3.0 ruled Neon loses here
val TrendDown = Color(0xFFE08A7D)        // warmed, softer than pure red

// === LEGACY TOKEN NAMES ===
// Kept as aliases pointing at the warm palette so every existing screen inherits the warmth
// without a 90-file rename. Prefer the warm names above in new code.
val ShopWhite = WarmWhite
val ShopOffWhite = WarmSurface
val ReceiptBeige = WarmBorder
val ReceiptBeigeDark = WarmBorderStrong
val ShopBlack = Ink
val ShopBlackSecondary = InkSecondary
val ShopBlackMid = InkMid
val ShopBlackTertiary = InkTertiary
val RankGold = WarmGold
