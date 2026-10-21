# Handoff: ShopMate POS — phone rail + tablet sale ticket

## Overview

A redesign of the ShopMate cashier-facing surfaces for both device buckets, plus the
screens CLAUDE.md lists as "not built yet" (Stock rework, Products margin health, stock
count sessions, unknown-barcode inbox, owner digest).

Two things are genuinely new and need a decision before anyone writes Kotlin:

1. **The phone app frame.** `AppSidebar` is specified at 84dp on all device sizes. On a
   390dp-wide phone that leaves 306dp of sheet. This design narrows it to **64dp with
   labels on long-press** for COMPACT only. **Already merged** — `CLAUDE.md` in this folder
   is v3.1 and carries the amended § Navigation text. Copy it over the repo's file before
   building.
2. **The tablet tender flow.** `PosScreenTablet.kt` has a charge button and a cash-change
   calculation but no tender state. This design adds a three-phase machine
   (`SALE → TENDER → DONE`) inside the existing 240dp ticket, with no layout reflow.

Everything else in this bundle is an application of decisions already in CLAUDE.md.

## About the design files

`ShopMate Phone.dc.html` in this bundle is a **design reference created in HTML** — a
prototype showing intended look and behaviour. It is not production code and nothing in it
should be ported directly.

The task is to **recreate these designs in the existing Android codebase**: Jetpack
Compose, MVVM + Repository, Hilt, Room, the tokens in `ui/theme/Color.kt` and `Type.kt`,
and the component library in `ui/components/`. Where the HTML and an existing composable
disagree on a value, **the composable wins** unless this document says otherwise — the
divergences are listed explicitly in § Deliberate divergences.

Open the file in a browser. The right-hand index switches screens; the two tabs above the
tablet frame switch between Sale and Stock.

## Fidelity

**High-fidelity.** Colours, type, spacing and interaction are final. Values below were
lifted from the existing Kotlin source (`ProductGridCard.kt`, `NumberPad.kt`,
`SaleLineItem.kt`, `ChargeButton.kt`, `ProductGrid.kt`, `ProductSearchBar.kt`,
`SaleSuccessBanner.kt`, `AppSidebar.kt`, `PosScreenTablet.kt`, `Color.kt`) and should
match on screen. New components are specified to the same precision.

One caveat: the HTML renders in CSS px against a 390×844 and 1180×820 frame. Read every
number below as **dp / sp**, not px.

---

## Build order

The UI is not the first task. This order is forced by § Schema debt.

1. **`MIGRATION_3_4`** — Products and Stock cannot be honest without
   `previous_cost_price`, `cost_price_changed_at`, `product_barcodes`,
   `unknown_barcodes`, `stock_counts`, `stock_deltas.count_session_id`. SQL is already
   written in CLAUDE.md § Schema debt. Ship it with `BarcodeNormaliser` (UPC-A → EAN-13)
   in the same commit.
2. **Target margin setting** — `SettingsRepository`, DataStore, default 30%. Blocks
   `MarginGauge`; it has no notch without it.
3. **Component family** — `RunwayGauge`, `MarginGauge`, `InsightRow`, `SectionHeader`,
   `LedgerRow`, `StoppedChip`. Build `InsightRow` once for both Stock and Products.
4. **`ScanSource` abstraction** — the scanner overlay and the unknown-barcode path both
   depend on it. HID keyboard source first; CameraX + ML Kit behind the same interface.
5. **Screens**, in this order: tablet POS tender → phone POS + rail → Stock → stock count
   → Products → barcode inbox → owner digest.

Steps 1–4 are roughly 60% of the work and none of it is visible.

---

## Design tokens

All values already exist in `ui/theme/Color.kt` unless marked new.

| Token | Hex | Use in this design |
|---|---|---|
| `TradeGreen` | `#005B52` | sidebar canvas, secondary buttons, scan icon, filled runway segments |
| `CheckoutNeon` | `#99FFAA` | brand mark, selected nav pill, Charge, Commit count, success banner edge |
| `ShopBlack` | `#1A1A1A` | primary text, Stopped chips, phone cart bar, selected filter chip |
| `ShopBlackMid` | `#6B6459` | secondary text, meta lines |
| `ShopBlackLight` | `#8A8175` | eyebrow labels, tertiary meta |
| `ReceiptBeige` | `#ECE7E0` | hairlines, dividers, card borders |
| — | `#F0EDE7` | search field fill, number-pad press state, selected ticket line |
| `OffWhite` | `#FBF9F6` | cart panel, surface cards |
| `ShopWhite` | `#FDFCFA` | content sheet |
| `StockOrange` | `#E07B39` | Warning tier, partial runway day, margin overflow, pending sync dot |
| — | `#FBF0E7` | Warning tint surface (`StockOrange` at low alpha) |
| — | `#E8FFF0` | success banner fill (`CheckoutNeon` subtle) |
| `TrendUp` | `#2E7D32` | trend arrows, healthy stock counts |
| `RankGold` | `#C9A227` | rank 1 in Top sellers |
| — | `#A89F94` | disabled pad keys, placeholder text |

Placeholder hexes above marked `—` should be resolved to existing tokens in `Color.kt`
where one exists; add named tokens rather than inlining if not.

**Spacing** — 4dp base. Screen padding 16dp. Card padding 12dp. Row padding 12/14dp.
Gaps: 1dp between list rows (they share a tint), 5dp number pad, 8dp phone grid,
10–12dp tablet grid.

**Radii** — 9dp pad keys, 10dp header icon buttons, 12dp cards / rows / search / pad
container, 14dp Charge + surface cards, 15–16dp pills and chips, 22dp bottom sheets,
24dp sheet top-start corner.

**Type** — Inter, three weights only, never Bold. Eyebrow labels 10.5sp / Medium /
0.11em / uppercase / `ShopBlackLight`. All figures `tnum`.

---

## Screens

### 1. Phone app frame — `AppSidebar` COMPACT variant

**Purpose:** keep the green frame identity on a 390dp screen without eating the sheet.

**Layout.** 64dp fixed-width Trade Green column, full height. Brand mark 38dp, 13dp
radius, Checkout Neon fill, black cart glyph, 10dp from top. 20dp gap. Destinations in a
scrollable middle column, Settings pinned to the bottom — unchanged from the 84dp variant.

**Items.** Each item is a 46×30dp pill, 15dp radius, centred in a full-width 44dp touch
row (7dp vertical padding). Selected: `CheckoutNeon` fill, `ShopBlack` icon at 20dp.
Unselected: transparent, white icon at 72%. Animate the fill with `animateColorAsState`,
180ms — same as the existing sidebar.

**No labels.** Labels appear on **long-press** as a `ShopBlack` tooltip, 12sp Medium,
`ShopWhite` text, 6/11dp padding, 8dp radius, positioned to the right of the pressed
item and vertically centred on it. 320ms press delay. Dismiss on release.

**Badge.** Products carries a `StockOrange` count badge, 16dp min-width, 8dp radius,
white 10sp SemiBold, top-right of the pill — the unknown-barcode count.

**Specified in `CLAUDE.md` v3.1** § Navigation & app frame, as the COMPACT variant. v3.0
said 84dp on all sizes with no mention of labels.

### 2. Phone POS — `pos`

**Question:** how fast can I take this money?

**Header** (`ContentHeader`, 60dp): store name, sync dot + "2 pending", one action icon
(receipt → `sales`). The scan trigger is **not** here — it lives in the search bar, per
`ProductSearchBar.kt`.

**Search bar.** 44dp, 12dp radius, `#F0EDE7` fill, 12dp horizontal padding. Leading
search glyph 17dp `ShopBlackLight`, placeholder "Search products" 14sp `#A89F94`,
trailing scan icon 20dp in `TradeGreen` inside a 28dp touch target.

**Category chips.** `ShopMateChip`, 30dp tall, 13dp horizontal padding, 15dp radius,
6dp gaps, horizontally scrollable, bleeding to the screen edge. Selected: `ShopBlack`
fill, `ShopWhite` text. Unselected: transparent, 1dp `#DDD6CC` border, `ShopBlackMid`.

**Product grid.** `ProductGrid.kt` is `GridCells.Adaptive(120.dp)` with 12dp gaps and
16dp padding — use it unchanged. Cards are `ProductGridCard`: 116dp tall, 12dp radius,
12dp padding, `ShopWhite` fill, 1dp `ReceiptBeige` hairline, no elevation. Name 14sp
SemiBold, single line with ellipsis, 14dp right padding to clear the dot. `CategoryDot`
10dp, top-end 12dp. Bottom block: price 15sp Medium, then either a `StockOrange` /
`TrendUp` stock line at 11.5sp Medium ("12 btl") or a `StoppedChip` reading "Out" —
20dp tall, 10dp radius, `ShopBlack` fill, `ShopWhite` 11sp Medium.

**Cart bar.** Pinned to the bottom over a `ShopWhite` gradient fade. 60dp, 16dp radius,
`ShopBlack` fill, 16dp left / 8dp right padding. Left: eyebrow "N items in this sale"
then total 19sp SemiBold `ShopWhite`. Right: a 44dp `CheckoutNeon` pill, 12dp radius,
"Charge" 15sp SemiBold + chevron. Tapping anywhere opens the tender sheet.

**Tender sheet.** Bottom sheet, 22dp top radius, max 96% height. Cart lines (26dp qty
chip, name, line total) → `PaymentMethodCard` row (Cash / EcoCash / Card, 38dp, 11dp
radius, selected fills `TradeGreen`) → Cash in / Change out pair → number pad → Charge.
Change out flips to "Still owing" on a `#FBF0E7` surface in `StockOrange` when the
tendered amount is short.

**Number pad.** From `NumberPad.kt`: 3 columns, 5dp gaps, keys 46dp tall, 9dp radius,
`ShopWhite` fill, 1dp `ReceiptBeige` border, 19sp Medium, `#F0EDE7` on press. "." and
backspace render in `#A89F94`. Haptic on every key (`HapticFeedbackConstants.KEYBOARD_TAP`).

**Receipt.** Full-screen. `SaleSuccessBanner` treatment at the top of a centred block:
12dp radius, `#E8FFF0` fill, 4dp `CheckoutNeon` left edge, 22dp `TradeGreen` check,
"Charged $X" 15sp SemiBold over receipt meta. Below: change 38sp SemiBold, then the
offline reassurance row — "Held on this device until you're back on a network", 7dp
`StockOrange` dot, never error styling. Actions: "Next sale" (`ShopBlack`, 54dp) and
"Print receipt" (text).

**Scanner overlay.** Dark scrim, corner brackets in `CheckoutNeon`, sheet below. Two
outcomes must both be demonstrable: a known code adds the product; an unknown code
records to `unknown_barcodes` and **the sale continues**. Copy: "The sale continues. The
code is saved to unknown codes — name it when you have a minute."

### 3. Tablet POS — `PosScreenTablet` / EXPANDED

**Three fixed zones, no reflow between Medium and Expanded.** Product area (flexible) ·
1dp `ReceiptBeige` divider · 240dp sale ticket. The 84dp labelled sidebar and the header
with the centred live clock are unchanged from the current implementation.

**Ticket.** Header "Current sale" 14sp Medium + item count. Then:

- **Lines** (`SaleLineItem.kt`): 8dp radius, 8dp horizontal / 7dp vertical padding, name
  13sp Medium, breakdown "2 × $1.10" 11sp in `#A89F94` beneath. Selected line tints to
  `ReceiptBeige` at 45%, 160ms. **No quantity chip** — the quantity lives in the
  breakdown, and while being typed it renders in `TradeGreen` SemiBold.
- **Hint / apply row.** Idle: "Tap a line, then type a quantity". While typing: the
  pending quantity plus a "Set quantity" outlined button, 28dp, `TradeGreen`. Quantity
  editing must **not** be bound to Charge.
- **Tender block** (visible in TENDER only): Cash in / Change out pair, 10dp radius,
  17sp SemiBold, same semantics as the phone.
- **Number pad**, always visible, keys 38dp on tablet. Routes to the quantity buffer in
  SALE and to the cash buffer in TENDER.
- **Total** 24sp Medium, then **Charge**: `ChargeButton.kt` geometry — 14dp radius, 18dp
  padding, 17sp Medium — filled `CheckoutNeon`, disabled at 38% alpha with an empty cart.

**Payment method — not in the mock, required in the build.** The phone tender has
Cash / EcoCash / Card; the tablet ticket does not yet. Add a `PaymentMethodCard` row
directly above the Cash in / Change out pair, visible only in TENDER, three across at
240dp. Cash is the default. Selecting Card or EcoCash hides the Cash in / Change out
pair (there is no change to give) and Charge commits directly. `PaymentMethod` already
carries `MOBILE_MONEY` and `BANK_TRANSFER` — do not surface them at the till without a
decision; the enum having five values is not a reason for five buttons.

**Success.** `SaleSuccessBanner` inline at the top of the **product area** (where
`ProductArea` already places it), max 480dp wide — not a full-screen takeover. The tablet
cashier stays in the grid.

### 4. Stock — `stock`

**Question:** when do I run out?

**Week-ahead strip** (`WeekAheadStrip`): 7 columns, 44dp (phone) / 56dp (tablet) tracks,
6dp radius, `#F0EDE7` rail with the bar **bottom-anchored** so heights compare. Bars in
`TradeGreen`, `StockOrange` for days where items run out, `ShopBlack` for the last day.
Day label + count beneath. Summary line below in `ShopBlackMid`: "Six items run out
Wednesday and Thursday. Two are already out."

**Sections**, in this order, each a `SectionHeader` (eyebrow + count): Runs out this week ·
Stopped · Healthy · Not moving. Rows are `InsightRow` — name + meta, `RunwayGauge`
(52×16dp, 7 segments, 2dp gaps, filled `TradeGreen`, partial day `StockOrange`, hollow =
1dp `ReceiptBeige` outline), trailing value + sub. Warning rows sit on `#FBF0E7`.
Stopped rows show a `StoppedChip` instead of a value. Dormant rows render the name in
`ShopBlackLight`.

**Velocity confidence.** Items with `confident = false` must render **no runway** — all
segments hollow — and read "not enough history yet". Never project from thin data.

**FAB.** "Count stock" extended FAB, 48dp, 14dp radius, `TradeGreen`, bottom-right.

**Detail (EXPANDED only).** 340dp right-hand panel pinned to the sheet edge — the same
composable the phone shows as a bottom sheet, per § Adaptive layouts. Header: name,
meta, an 80dp runway, runway text. Then **"Why it says 6"** — a `LedgerRow` list: what ·
when · signed delta · running balance. Restocks in `TrendUp`, count corrections in
`StockOrange`, sales in `ShopBlack`. This is the § brand promise "why does it say 6?"
requirement made literal. Actions: "Add to order list" (`TradeGreen`) and "Adjust".

`actor` stays null until auth ships — **render no attribution**, not a placeholder name.

### 5. Products — `products`

**Question:** am I still making money on this?

Same `InsightRow`, `MarginGauge` in the gauge slot: one continuous 52×16dp track, cost
filling from the left in `TradeGreen`, a 2dp `ShopBlack` notch at the store's target
margin, overflow past the notch in `StockOrange`, and the whole track `ShopBlack` when
selling at or below cost.

Sections: Cost went up · Selling at a loss · Holding target. Meta lines carry the actual
movement — "cost $1.95 → $2.12 on 28 Aug" — which is what `previous_cost_price` and
`cost_price_changed_at` are for.

**Barcode inbox entry.** A `#FBF0E7` card at the top: "3 codes scanned but not named" /
"They sold anyway. Name them when you have a minute." → `barcode_inbox`.

### 6. Stock count session — `stock_count`

Three summary cards (counted · don't match · short), then rows with a `QuantityControl`
stepper. Variance rows tint `#FBF0E7`; meta reads "expected 12 · +2". Commit button
`CheckoutNeon`, 52dp, and the verb carries through: "Commit count" → "Counted 5 items".

A commit is a **batch of signed deltas**, never an assignment — architecture rule 5.

### 7. Unknown barcodes — `barcode_inbox`

Cards with the code 15sp SemiBold `tnum`, meta "7 hits · first seen 28 Aug · till 2", and
a "Name it" outlined `TradeGreen` button routing to `product_add` prefilled with the code.

### 8. Owner daily digest

**Not routed, and not to be routed.** `CLAUDE.md` v3.1 resolves this: the owner dashboard
ships beside the app, later. The design is included as a reference for the remote-monitoring
user only — do not add a sidebar destination for it.

`TradeGreen` hero card with yesterday's total, then "Worth knowing" — pattern cards, not
event cards. "Three short counts on the morning shift" is the § brand promise "surface
patterns, not just events" rule; it is blocked on auth for the shift attribution.

### 9. Sign in

PIN entry, 4 slots at 52dp, 3-column keypad. Speculative — auth is unscheduled. Do not
build ahead of the auth decision.

---

## State — tablet POS

The one piece of genuinely new state logic.

```kotlin
enum class TicketPhase { SALE, TENDER, DONE }

data class PosTabletUiState(
    val phase: TicketPhase = TicketPhase.SALE,
    val selectedLineIndex: Int = 0,
    val quantityBuffer: String = "",     // SALE — digits only, max 3
    val cashBuffer: String = "",         // TENDER — one decimal point, max 7
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
)
```

Transitions:

| From | Trigger | To | Effect |
|---|---|---|---|
| SALE | Charge, cart non-empty | TENDER | clears `quantityBuffer` |
| SALE | "Set quantity", buffer non-empty | SALE | applies buffer to selected line, min 1 |
| SALE | tap a line | SALE | selects it, clears buffer |
| TENDER | Charge | DONE | `recordSale()` — single `@Transaction`, rule 3 |
| DONE | Charge ("Next sale") | SALE | clears cart, cash buffer, banner |

Charge is disabled when the cart is empty. The number pad writes to `quantityBuffer` in
SALE and `cashBuffer` in TENDER — one pad, two destinations, chosen by phase.

Change is derived, never stored: `cashBuffer.toBigDecimalOrNull() - total`. Negative
renders as "Still owing". Card and EcoCash skip the cash pair entirely.

Cart lines stay in-memory (`CartItem` is never persisted, per § Domain models) — killing
the app mid-sale loses the cart, which is the existing accepted behaviour.

---

## Interactions

- **Nav selection** — `animateColorAsState`, 180ms, on the pill fill only.
- **Long-press label** — 320ms delay, appears instantly, dismisses on release.
- **Line selection** — 160ms tint.
- **Number pad** — haptic on every key.
- **Charge** — no loading state; the sale is a local transaction and completes
  immediately. The banner is the confirmation. Sync happens later and silently.
- **Offline** — the sync dot goes `StockOrange` and the header reads "N pending". Never
  a snackbar, never an error colour, never a retry prompt at the till.

## Deliberate divergences from existing source

Three. All are now ratified in `CLAUDE.md` v3.1 — build to the design, not to the old code.

1. **`ChargeButton.kt` fills `TradeGreen`; this design fills `CheckoutNeon`.** § The
   Checkout Neon rule names the POS Charge button as a sheet commit. The geometry (14dp
   radius, 18dp padding, 17sp Medium, 38% disabled) is taken from the source unchanged.
2. **Out of stock renders as a `ShopBlack` chip, not `StockRed`.** § Severity retires red
   for business conditions — an empty shelf is stopped, not broken.
3. **The phone sidebar is 64dp, not 84dp.** See § 1.

## Files

- `ShopMate Phone.dc.html` — the full design. Right-hand index switches screens; the tabs
  above the tablet frame switch Sale / Stock.
- `support.js` — runtime for the above. Keep it beside the HTML.
- `CLAUDE.md` — **v3.1, the merged file.** Replaces the repo's `CLAUDE.md` wholesale: phone
  rail variants, tablet tender phases, component specs, `ChargeButton` recolour, Supabase
  backend, auth timing, target margin 30%. All four v3.0 open questions are resolved.
  Copy this in first; everything below builds against it.
- `CLAUDE.v3.1.md` — the amendment draft, kept as a record of what changed and why. Not
  needed to build.

## Assets

None. Every icon in the design is an inline SVG drawn as a stand-in — use the project's
existing icon set (Material Symbols or whatever `AppSidebar.kt` currently pulls from)
rather than tracing these. No images, no photography, no logos.
