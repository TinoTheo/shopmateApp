# Handoff: ShopMate brand in the app (rail mark, app icon, splash, sign-in, receipts, owner digest)

## Overview
This bundle puts the ShopMate logo into the Android app: the brand mark at the top of `AppSidebar`, the launcher icon, the splash, PIN sign-in, the printed and shared receipts, and the owner digest header. It follows on from `design_handoff_pos_mobile_tablet` and `CLAUDE.md` v3.1. Where this README doesn't cover something, those documents apply.

## About the design files
`ShopMate Brand Screens.dc.html` is a **design reference built in HTML**. It shows the intended look and behaviour and is not production code. Rebuild it in the existing codebase using Jetpack Compose, the tokens in `ui/theme/Color.kt` and `Type.kt`, and the components in `ui/components/`. Open the file in a browser and use the option IDs (`1c`, `2a`, …) to find each screen.

## Fidelity
**High-fidelity.** Layout, sizes and copy are final. The HTML has a few off-palette neutrals for canvas presentation. Map them to existing tokens using § Colour mapping. `CLAUDE.md` says that if a colour isn't in `Color.kt`, it doesn't exist.

## Chosen options
| Surface | Option | Status |
|---|---|---|
| Sidebar brand mark | **1c**: Neon circle | chosen |
| App icon | **2a**: Stacked Logo-40 on Trade Green | chosen |
| Splash (phone) | **1h**: oversized mark bleeding off-screen | chosen |
| Splash (tablet) | 1i: centred lockup | approved |
| Sign in | 1j phone / 1k tablet | approved (auth is still unscheduled, so build behind a flag or not at all) |
| Printed receipt | **2b**: 1m layout in black only | chosen |
| Shared receipt (WhatsApp image) | 1m | approved |
| Owner digest | 1n phone / 1o tablet | approved |

Ignore 1a, 1b, 1d, 1e, 1f, 1g and 1l. They were the options that weren't picked.

---

## Colour mapping
| HTML value | Use token | Notes |
|---|---|---|
| `#005B52` | `TradeGreen` | |
| `#99FFAA` | `CheckoutNeon` | Frame identity only: the rail mark, the digest header mark on tablet, the splash lockup "M" and tagline |
| `#D4CEC9` | `ReceiptBeige` | Hairlines, empty PIN dots |
| `#FDFCFA` | `ShopWhite` (`#FFFFFF`) on sheets; `ShopOffWhite` (`#FAF9F8`) on the digest card body | |
| `#1A1A1A` | `ShopBlack` | |
| `#6B6459`, `#8A8175` | `ShopBlackMid` / `ShopBlackLight` | |
| `#F0EDE7` | `ShopOffWhite` | PIN slots, digest till rows |
| `#E07B39` | `StockOrange` | "held" dot |
| `#FBF0E7` / `#F2E0CF` | `StockOrange` at 8% / 16% alpha | Warning card fill / border |
| `#014A43` | **New:** `TradeGreenDeep` | Only used for the oversized splash mark in 1h. Add it to `Color.kt` with a KDoc comment saying it is splash-only, or draw `ShopBlack` at 18% alpha over `TradeGreen`, which gives the same result. |

Unselected rail icons are white at 72% alpha, as before.

---

## 1. Sidebar brand mark: 1c
The brand mark sits at the top of `AppSidebar`, centred horizontally.

| | COMPACT (64dp rail) | EXPANDED (84dp rail) |
|---|---|---|
| Circle | 40dp, `CircleShape`, `CheckoutNeon` | 46dp, `CircleShape`, `CheckoutNeon` |
| M glyph | 19×26dp, `TradeGreen` | 22×30dp, `TradeGreen` |
| Top padding (below status bar) | 10dp | 14dp |
| Gap to first nav item | 18dp | 18dp |

- The circle is deliberately not the pill shape, so it can't be mistaken for the selected item. Don't round-rect it.
- It is not interactive and has no ripple. `contentDescription = "ShopMate"`.
- Draw the M as a `VectorDrawable` (`ic_shopmate_mark.xml`) tinted with `TradeGreen`. See § Assets.

## 2. App icon: 2a
This is an adaptive icon.
- **Background layer:** solid `TradeGreen`.
- **Foreground layer:** Stacked Logo-40 (Neon looped M with white "Shop / Mate" lettering), **centred on both axes** on the 108dp canvas. It must fit inside the 66dp safe-zone circle. Logo bounding box: **width 62dp** (aspect 1.458 : 1, so about 42.5dp tall), centred at (54, 54).
- **Monochrome layer** (Android 13 themed icons): the M mark only, 30dp tall, centred. The lettering is unreadable at themed-icon sizes.
- **Legacy / Play Store:** 512×512 PNG, Trade Green, logo 66% of the width, centred.
- At 40dp and below the lettering gets small. Check it on a real launcher. If it doesn't read, the fallback is the monochrome-layer art (M only) in Neon on Trade Green.

## 3. Splash: 1h phone, 1i tablet
Android 12+ `SplashScreen` only allows a centred icon on a flat colour, so this is done in two stages:
1. **System splash** (`Theme.SplashScreen`): `windowSplashScreenBackground = TradeGreen`, `windowSplashScreenAnimatedIcon` = the M mark in Neon, `postSplashScreenTheme` = app theme. It stays up for as long as the system holds it and needs no custom code.
2. **`BrandSplash` composable**, shown while Room opens, then crossfading (200ms) into `pos`. Only show it if opening takes longer than one frame. **No spinner, no progress bar.**

**Phone (1h), 390×844dp reference:**
- Full-bleed `TradeGreen`.
- Oversized M in `TradeGreenDeep`, 560×748dp box, offset x = −120dp, y = −40dp from the top-left. It bleeds off the left and top edges.
- Lockup, bottom-start: start 32dp, bottom 72dp (above the nav bar inset). Wordmark is 240×70dp: the "M" segment (first 53dp) in `CheckoutNeon`, the rest in `ShopWhite`.
- Tagline "uncompromising control", right-aligned under the wordmark with a −10dp overlap and 10dp end padding, in `CheckoutNeon`.

**Tablet (1i), 1180×820dp:**
- Lockup centred. Wordmark 460×135dp; tagline 34sp-equivalent with −18dp overlap and 20dp end padding.
- Footer centred 36dp from the bottom: "Mbare Market Stop · Till 1 · v3.1", 12sp Regular, `ShopWhite` at 60%, `tnum`.

**Typography conflict:** the tagline in the mock is set in EB Garamond Italic, but `CLAUDE.md` bans serifs and downloadable fonts. **Don't bundle a serif.** Export wordmark + tagline as one vector lockup (`ic_shopmate_lockup.xml`) and draw it as an image. The tagline is logo artwork, not UI text.

## 4. Sign in: 1j phone, 1k tablet
Auth is unscheduled (see the POS handoff, § 9). Build behind a feature flag or not at all.

**Phone (1j):**
- Green frame header, 180dp tall, lockup 200×59dp starting at 28dp.
- White sheet with a 24dp top-start radius, padding 32dp top and 24dp sides.
- Title "Open the till": 22sp SemiBold, −0.02em tracking.
- Subtitle "{store} · Till {n}": 14sp Regular, `ShopBlackMid`.
- PIN slots: 4 × 52dp tall, equal width, 10dp gap, 12dp radius, `ShopOffWhite`. Each has an 11dp dot: `ReceiptBeige` when empty, `ShopBlack` when filled, `TradeGreen` on success.
- Helper text, 12sp, 10dp below the slots: "Enter your 4-digit till code" in `ShopBlackLight`; "Opening the till" in `TradeGreen` on success.
- Keypad: 3 columns, 8dp gap, 52dp keys, 12dp radius, `ShopWhite`, 20sp Medium `tnum`. The bottom row is blank, 0, ⌫ (⌫ in `ShopBlackLight`). Pressed state: `ShopOffWhite`.

**Tablet (1k):**
- Split screen: green panel on the left (flex, 56dp padding, lockup 340×100dp), white sheet on the right (fixed 480dp, 24dp top-start radius, padding 72/48dp).
- Keys are 56dp tall. Title is 24sp.
- The green panel's foot shows the held-sales line, 13sp `ShopWhite` 72% with a 6dp `StockOrange` dot: "2 sales held on this device from yesterday". This is the one place the line appears before sign-in. Per Copy rule, only show it when the count is above 0.

**Behaviour:**
- Digits fill left to right; ⌫ deletes one.
- On the 4th digit, verify. On success, dots turn green, wait 250ms, then navigate.
- On failure: one 120ms horizontal shake on the slot row and the helper text "That code didn't match. Try again." in `ShopBlack`. Never `ErrorRed`, never "Invalid".

## 5. Receipts
### 5a. Printed: 2b, 58mm thermal
Printers may be monochrome, so **black only**, with no grey fills. Render the receipt as a 1-bit bitmap, **384px wide** (58mm at 203dpi), and send it as ESC/POS raster. Rendering Inter text in the bitmap keeps the brand type on any printer.

| Element | Spec (at 384px wide, ~1.65× the mock) |
|---|---|
| Header band | Solid black, full width, 20px padding. Inside, knocked out in white: "RECEIPT" eyebrow (Medium, 0.11em, uppercase); store name (SemiBold, largest text on the receipt); "#{receipt} · {d MMM}, {HH:mm}" |
| Mark | White M, top-right of the band, 25×33px |
| Lines | Name (Medium) and qty line "2 × $1.10" (Regular) on the left, line total (Medium) on the right. 1px **dashed** black divider under each line |
| Totals | "Total" label left, amount right in SemiBold, about 1.6× body size. Below it: "Cash $10.00" left, "Change $1.30" right |
| Footer | 1px solid rule, then the centred M (13×18px) and "Kept by ShopMate" |

- All figures use `tnum`.
- If the sale was taken offline, add "Held on this device. Sends when you're back." above the footer (per `CLAUDE.md`, the receipt is one of the places this line belongs).

### 5b. Shared: 1m, WhatsApp image
- 320dp-wide card, 18dp radius, rendered to PNG at 3×.
- `TradeGreen` header with "RECEIPT" eyebrow in Neon, store name 18sp SemiBold in `ShopWhite`, meta 12sp in `ShopWhite` 72%, and a Neon M (24×32dp) at top-right.
- Body is `ShopWhite`: lines with a 1dp `ReceiptBeige` divider, total at 24sp SemiBold.
- Footer: Trade Green M (9×12dp) and "Kept by ShopMate", 11sp `ShopBlackLight`.
- Share it through the system share sheet.

## 6. Owner digest: 1n phone, 1o tablet
This ships beside the till app, not inside it. Content and rules are in the POS handoff, § 8.

**Phone (1n):**
- White surface with no rail.
- 60dp header: Trade Green M (20×27dp), then the store name (16sp SemiBold) and date (11.5sp `ShopBlackMid`). 1dp `ReceiptBeige` bottom border.
- Hero card: 16dp radius, `TradeGreen`, 18dp padding. "YESTERDAY" eyebrow in Neon, amount 34sp SemiBold, meta "168 sales · closed 18:04".
- "WORTH KNOWING" eyebrow, then cards: 14dp radius, 13dp padding, title 14sp Medium, body 12.5sp. Warnings use the `StockOrange` tints.

**Tablet (1o):**
- 76dp Trade Green header band with a Neon M (24×32dp) and white store name and date.
- White sheet, 24dp top-start radius.
- Two columns: a fixed 400dp column (hero at 44sp, then the "TILLS" list) and a flexible "Worth knowing" column. 32dp gap.

**Open copy question:** the Till 2 row reads "closed 17:52 · 2 sales held". If a till still has held sales, yesterday's hero total is incomplete. Proposed fix: when any till has held sales, the hero meta reads "168 sales so far · 2 still on Till 2", and the row dot is `StockOrange`. **Confirm with design before building.**

---

## Assets
In `assets/`:
- `mark.png`: the looped M, used as an alpha mask in the HTML.
- `wordmark.png`: horizontal wordmark.
- `stacked-40.png`: Stacked Logo-40, trimmed to its bounds (1200×823) so it centres exactly.

These are raster references only. **Ask the brand owner for SVG or AI masters** and convert them to VectorDrawables:
- `ic_shopmate_mark.xml`
- `ic_shopmate_lockup.xml` (wordmark + tagline)
- `ic_launcher_foreground.xml` (stacked logo)
- `ic_launcher_monochrome.xml`

Don't trace the PNGs by hand. The original files are in `uploads/ShopMate Logos/` in the design project.

## Files
- `ShopMate Brand Screens.dc.html`: every option. Turn 2 (top) has 2a and 2b; Turn 1 has 1a–1o.
- `assets/`: logo art referenced by the HTML.
- `support.js`: runtime for opening the HTML locally. Not part of the build.

## Suggested Claude Code prompt
> Read `CLAUDE.md`, then `design_handoff_brand_screens/README.md`. Implement in this order: (1) vector assets + `Color.kt` addition, (2) `AppSidebar` brand mark per 1c, (3) adaptive launcher icon per 2a, (4) SplashScreen + `BrandSplash`, (5) printed receipt bitmap renderer per 2b, (6) shared receipt image per 1m. Leave sign-in and owner digest until auth is scheduled. Don't change any token or rule in `CLAUDE.md` without flagging it.
