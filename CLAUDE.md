# ShopMate Android — CLAUDE.md

**v3.1 · 2026-09-02** — merges the amendments the POS phone/tablet design work exposed:
the sidebar gains a COMPACT width variant, the tablet sale ticket gains a tender phase,
`ChargeButton` is corrected to Checkout Neon, four to-build components get buildable
specs, and open questions 2 and 3 are resolved. v3.0's design decision layer is unchanged.

Every decision in this file is final. Read it before touching any code.

### What changed in v3.1

1. **`AppSidebar` has two width variants** — 64dp icons-only on COMPACT, 84dp icon+label
   on EXPANDED (§ Navigation). Still a green full-height sidebar, still no bottom bar.
2. **Tablet POS sale ticket is a three-phase state machine** `SALE → TENDER → DONE`, zones
   never reflowing (§ Adaptive layouts).
3. **`ChargeButton` fills `CheckoutNeon`.** The built composable filled `TradeGreen`; the
   colour table wins and the code is corrected (§ Colours).
4. **`RunwayGauge`, `MarginGauge`, `InsightRow`, `LedgerRow` have full specs** (§ Component
   library) — buildable without re-deriving sizes per screen.
5. **Offline reassurance is placed, not just worded** (§ Copy and voice).
6. **Open question 2 resolved:** owner dashboard ships beside this app, later.
   **Open question 3 resolved:** target margin default 30%.
7. **Backend resolved: Supabase.** § Backend & sync rewritten against Postgres + PostgREST
    + RLS with org → memberships → stores. The Django Retrofit layer is retired.
8. **Auth timing resolved: auth ships with the Supabase cutover**, before the ledger and
   count-history attribution features. `actor` stays null until then, never a placeholder.

### What changed in v3.0 — ratified

1. **Checkout Neon split into two families** — frame identity vs. content commits. Resolves
   the v2.1 conflict where Neon was both "nav pill / brand mark" and "success".
2. **Trend-up is `TrendUp`, not Checkout Neon.** v2.1 listed both. Neon loses.
3. **Gauges added as a canonical component family** (§ Gauges). New build.
4. **Severity language fixed at three tiers** and mapped onto existing semantic colours.
   No new colours.
5. **Target user updated** to reflect the shift to owners monitoring remotely. v2.1 still
   said "a single cashier". Confirm this.
6. **Schema debt section added** (§ Schema debt) — Room 3 → 4 migration required before
   Products/Stock can be honest.
7. **Backend section flagged** — resolved in v3.1: Supabase. See § Backend & sync.

---

## Project overview

ShopMate is an **offline-first Android POS and inventory app** for small retail shops in
Zimbabwe and South Africa. It records sales locally via Room, queues sync events, and
uploads when connectivity is available.

**Users, in priority order:**

1. **The cashier** at the till — phone or tablet, all day, needs speed above everything.
2. **The owner/manager** — often not on the premises, checks in from elsewhere, needs to
   know if anything is wrong before it costs money.

Every screen serves one of these two. If it serves neither, it does not ship.

Package: `com.shopmate`
Architecture: single-activity, Jetpack Compose, MVVM + Repository
minSdk 28 · targetSdk 35 · compileSdk 35

---

## Design decisions

This section exists so that a screen nobody has specified yet still comes out looking like
ShopMate. If a decision below conflicts with something you want to build, the decision wins
or the decision gets amended here first — not silently overridden in a composable.

### Every screen answers one question

A screen is named for the question a person asks, never for the table it reads from.

| Screen | The question | Measured in |
|---|---|---|
| POS | How fast can I take this money? | seconds |
| Stock | When do I run out? | time |
| Products | Am I still making money on this? | margin |
| Reports | What happened? | period |
| Sync | Where is my data? | queue depth |

Products and Stock read largely the same rows. They are two screens because they answer
two questions, not because `products` and `stock_deltas` are two tables. **Never split or
merge a screen on a database boundary.**

### The brand promise is honesty about state

"No data drops. No Friday surprises." Both taglines are about the app telling the truth
when the truth is inconvenient. This has concrete consequences:

- **Never show a number as certain when it isn't.** If a till has not reported, say so and
  name what is unknown. Do not quietly show a smaller total.
- **Offline is a state, not an error.** Offline copy is reassuring and specific. It never
  uses error styling, an alert icon, or an apologetic tone.
- **Never overwrite a count.** Stock is the sum of an append-only ledger. Any screen
  showing a stock number must be able to answer "why does it say 6?"
- **Surface patterns, not just events.** One short count is noise. Three in a row on the
  same shift is the thing worth showing.

### Gauges — one instrument, two units

Rows across Stock and Products carry a small gauge in the same slot, at the same size, so
they are read the same way. The instrument is shared; the unit changes with the screen.

- **`RunwayGauge` (time)** — seven discrete segments, one per day ahead. Filled while stock
  lasts, hollow after it runs out, the partial day in `StockOrange`. Days are discrete, so
  the gauge is segmented. Used on Stock.
- **`MarginGauge` (money)** — one continuous track. Cost fills from the left, the store's
  target margin sits as a notch, and cost crossing the notch renders the overflow in
  `StockOrange`. Selling at or below cost fills the whole track in `ShopBlack`. Money is
  continuous, so the gauge is continuous. Used on Products.

Both are 52dp wide, 16dp tall, and sit immediately left of the trailing value. **Do not
add a third gauge without amending this section.** The value of the system is that a
cashier learns to read one shape.

### Severity — three tiers, no new colours

| Tier | Meaning | Colour | Where |
|---|---|---|---|
| Healthy | nothing needed | `TradeGreen` / no treatment | default |
| Warning | will hurt soon | `StockOrange` + `StockOrange` tint surface | runs out this week, thin margin, cost rose |
| Stopped | already costing money | `ShopBlack` chip, white text | out of stock, selling at a loss |

**`ErrorRed` is for failures, not for business conditions.** An empty shelf is not an
error — it is a fact that costs money, and it reads as stopped, not broken. Nothing on
Stock or Products uses `ErrorRed`.

Dormant items (not sold in 14+ days) use `ShopBlackMid` at reduced emphasis — visible,
never urgent.

### Numbers

Every figure in the app — money, quantities, counts, percentages, times — renders with
tabular figures:

```kotlin
TextStyle(fontFeatureSettings = "tnum")
```

This lives on dedicated numeric styles in `Type.kt`, not repeated at call sites. A total
counting up must not shift the layout beside it. Money always goes through
`CurrencyFormatter` — never `String.format` at a call site.

---

## Colours — two-colour discipline

ShopMate uses **two brand colours only**: Trade Green means "attention here", Checkout Neon
means "something good happened, and it is done".
**If a colour isn't defined in `ui/theme/Color.kt`, it doesn't exist in ShopMate.**

| Name | Hex | Role |
|---|---|---|
| Trade Green | `#005B52` | Primary — app frame canvas, buttons, sync-ok dot |
| Checkout Neon | `#99FFAA` | Frame identity + content commits — see rule below |
| Receipt Beige | `#D4CEC9` | Structural — borders, hairlines, search field fill |
| Shop White / Off-White | `#FFFFFF` / `#FAF9F8` | Content sheet / cart panel |
| Shop Black (+2 tiers) | `#000000` / `#4A4A4A` / `#8A8A8A` | Text hierarchy |

Semantic colours (`StockGreen` / `StockOrange` / `StockRed`, `ErrorRed`, `TrendUp` /
`TrendDown`, `RankGold`) are functional, never decorative. Full palette in `ui/theme/Color.kt`.

### The Checkout Neon rule

Neon has exactly two jobs, and they live in different parts of the screen.

**In the frame** — permanent identity. The brand mark and the selected sidebar pill. These
are fixed, always present, and do not respond to what the user just did.

**In the content sheet** — a commit that succeeded and cannot be undone. Nothing else.
Per screen that is at most one or two places:

| Screen | Neon in the sheet |
|---|---|
| POS | the Charge button; the queue tick as it uploads; the confirmation banner edge |
| Stock | Commit count |
| Products | Save new price |
| Reports | none |

If a new element wants Neon and it is neither the frame nor a commit, it does not get it.
Selection states, hover, focus, trend arrows, and "good" values are **not** commits.
Trend-up uses `TrendUp`. Focus rings use `TradeGreen` (`CheckoutNeon` on the green frame).

Where a built composable contradicts this table, **the table wins and the composable is
corrected**. `ChargeButton` fills `CheckoutNeon`; its geometry (14dp radius, 18dp padding,
17sp Medium, 38% alpha disabled) is unchanged.

**Neon does not repeat within one commit path.** The tablet Charge button and the success
banner are the same commit at two moments — the banner's 4dp edge is Neon, its fill is
Neon-subtle, and nothing else in the ticket competes.

Add a KDoc comment on the `CheckoutNeon` token restating this rule so it survives.

---

## Brand

The looped M and the ShopMate wordmark come from `design_handoff_brand_screens/` (its README is
the spec; this section is what was decided). Logo art is used as supplied — **never redrawn or
traced by hand**.

- **Art lives in `res/drawable-nodpi/`** as raster for now: `ic_shopmate_mark`,
  `ic_shopmate_wordmark` (white alpha masks, tinted at draw time) and `ic_shopmate_stacked`
  (full colour, launcher only). When the brand owner's SVG masters arrive they become
  VectorDrawables under the **same names**, and no code changes.
- **Draw the logo through `ShopMateMark` / `ShopMateWordmark`** (`ui/components/ShopMateLogo.kt`),
  never an ad-hoc `Image`. The wordmark's leading M is `CheckoutNeon`, the lettering `ShopWhite`,
  split at 53/240 of its width — frame use only.
- **Sidebar mark (1c):** a `CheckoutNeon` circle with the M in `TradeGreen` — 40dp / 19×26dp M on
  COMPACT, 46dp / 22×30dp on EXPANDED, 18dp above the first destination. A circle, never the
  pill shape, so it can't read as a selection. Not interactive.
- **Launcher icon (2a):** adaptive. `TradeGreen` background, the stacked logo 56dp wide centred
  on the 108dp canvas, and a monochrome layer of the M alone for themed icons. Both layers are
  square PNGs per density in `mipmap-*dpi/` with the art already placed — an `<inset>` around the
  logo was scaled non-uniformly by launchers and squashed it. Play Store art is
  `app/src/main/ic_launcher-playstore.png`.
- **Splash:** Android 12+ always shows a system splash before any app code runs. Its icon is the
  white M (`ic_splash_mark`, from the brand owner's ShopMate Logo Mark-35.svg), 40% of the icon's
  height on a square canvas so it can't be stretched. `BrandSplash` follows on the same green and
  fades in the full lockup with its tagline (400ms), holds for 2 seconds in all (as WhatsApp
  does), then crossfades (200ms) into the app. Store and version at the tablet's foot. **No
  spinner, no progress bar.** `TradeGreenDeep` is no longer drawn anywhere; keep it only if a
  splash needs it again.
- **The tagline "uncompromising control" is serif logo art.** It ships inside the lockup vector
  (`ic_shopmate_lockup`, converted from the brand owner's ShopMate Logo-25.svg) and is never set
  as UI text (§ Typography bans serifs and downloadable fonts).
- **Receipts:** the store is the headline, ShopMate signs off at the foot ("Kept by ShopMate").
  `ReceiptContent` resolves the words once; `SharedReceiptImage` renders the 1m card (shared as a
  PNG through the share sheet from the receipt sheet) and `PrintedReceiptSlip` the 2b 58mm slip,
  black only, sent as ESC/POS raster via `print/EscPosRaster`. A sale still on the device adds
  "Held on this device. Sends when you're back." Cash handed over isn't stored, so receipts show
  "Paid via {method}", never invented cash/change amounts.

---

## Typography

**Inter is the single app typeface.** One family, three weights, bundled locally:

- `inter_regular.ttf` (400) — body text
- `inter_medium.ttf` (500) — titles, labels, headers
- `inter_semibold.ttf` (600) — display numbers, section headers, emphasis

No secondary display font. No serif. **Never Bold (700)** — the jump from Medium to
SemiBold carries all the hierarchy the app needs. Fonts are bundled TTFs in `res/font/` —
**no downloadable fonts**, a shop opening offline must not fall back to Roboto. Full M3
type scale in `ui/theme/Type.kt`.

**Eyebrow labels** (section headers on Stock and Products) are 10.5sp / Medium /
`0.11em` tracking / uppercase / `ShopBlackMid`. This is a distinct style in `Type.kt`, not
an inline `Modifier` stack.

---

## Copy and voice

Words are design material. They are specified here for the same reason spacing is.

- **Name things by what the person controls**, never by how the system is built. "3 sales
  held on this device", not "3 unsynced events".
- **Actions keep their verb through the whole flow.** `Charge` → `Charged`.
  `Commit count` → `Counted 12 items`. `Save new price` → `Priced at $2.60`.
- **Buttons say what happens**, not what widget they are. Never `Submit`, `OK`, `Confirm`.
- **Empty states are invitations.** "No items yet. Tap a product to start the sale." Never
  "No data" or an apology.
- **Failure states say what happened and what to do**, in the interface's voice. They do
  not apologise and they are never vague.
- **Offline copy is calm and specific.** "Held until you're back", not "Failed to sync".
- **Sentence case everywhere.** No title case on buttons or headers.
- **Be specific over clever.** "Runs out Thursday" beats "Low stock". "$6.20 short" beats
  "Discrepancy detected".
- **Offline reassurance is placed, not just worded.** The "held on this device" line belongs
  on the surface where the money was taken — the receipt, the queue — and nowhere else.
  Repeating it on every screen turns a calm fact into a nag, which is the same failure as
  error styling by a different route.
- **Strings come from the copy pass** (`design_handoff_copy_pass/`, merged into
  `res/values/strings.xml`). Counts go through `<plurals>`, never `"$n items"`. A screen that
  needs words with no key gets them flagged and added there first, not written in a composable.

### States the copy pass fixed

- **Stock run-out is one `RunOut`** (`domain/model/RunOut.kt`), computed on calendar dates
  and read by the list, the detail panel, the week-ahead strip and its caption alike — never
  re-derived per surface. `NoHistory` and `Dormant` are states, not missing values.
- **Sync has one status card** with a fixed priority: paused (signed out) → not sent
  (refused) → offline → waiting → all sent. A refused row says why (`SyncFailureReason`:
  receipt number taken, not allowed, other) and expands to Details. `Send now` is hidden
  offline — it never waits on a spinner.
- **The header status** is built from segments (paused or offline, waiting, not sent) with a
  hollow dot when the till can't send.
- **Sign-in and onboarding** sit on a Trade Green `BrandBand` with the stacked logo and a
  white sheet, like the app frame.
- **Reports compare like with like** (`ReportWindow`): an open period is compared with the
  same span up to the same time of day, and each period names its own comparison. The open
  hour or day is drawn lighter with a caption, so it is never read as a slow one.

---

## Navigation & app frame

The app frame (`ui/navigation/NavGraph.kt`) is **not** a bottom-nav Scaffold. It is:

- A **full-height Trade Green sidebar** (`AppSidebar`) on the left, on **all** device
  sizes: Checkout Neon brand mark on top, main destinations in a scrollable middle column,
  **Settings pinned at the bottom**. Two width variants, chosen by `DeviceLayout` — never
  a third:
    - **COMPACT — 64dp, icons only.** Destination labels appear on **long-press** as a
      `ShopBlack` tooltip anchored to the right of the pressed item. Selection is carried
      entirely by the Checkout Neon pill.
    - **EXPANDED — 84dp, icon over label.** Labels always visible.

  The rail is the only part of the frame that changes with device size. It is still a
  sidebar, still green, still full height.
- Selected item: **black icon on a Checkout Neon pill**; unselected: white icon at 72%
  opacity. Selection animates with `animateColorAsState`.
- Content lives on a **white sheet with a rounded top-start corner (24dp)** floating on the
  green canvas. The green fills the status-bar strip (white system icons); the sheet shows
  through the bottom bar (dark icons). Edge-to-edge.
- A shared **`ContentHeader`** (60dp, inside the sheet) replaces any TopAppBar: store name
  as the title on main destinations (sub-screens get a back arrow and their own title), a
  **sync-status dot** ("Synced" / "N pending"), per-route action icons, and on tablets a
  **centered live clock**.

**There is no bottom navigation bar in ShopMate, on any device size.** Any design reference
showing one is wrong and should be read with the sidebar substituted. Primary screen
actions are an extended FAB or an inline action in the content sheet — never a second
bottom bar competing with the frame.

### Routes

| Screen | Route | Sidebar |
|---|---|---|
| POS | `pos` | yes — start destination |
| Products | `products` | yes |
| Stock | `stock` | yes |
| Reports | `reports` | yes |
| Sync | `sync` | yes |
| Settings | `settings` | pinned at sidebar bottom |
| Sales History | `sales` | no — receipt icon on POS header |
| Categories | `categories` | no — icon on Products header |
| Add / Edit Product | `product_add` / `product_edit/{productId}` | no — from Products |
| Stock count session | `stock_count` | no — from Stock FAB |
| Unknown barcodes | `barcode_inbox` | no — from Products header badge |

---

## Adaptive layouts (WindowSizeClass)

`ui/util/DeviceLayout.kt` maps `WindowWidthSizeClass` to a deliberate two-bucket switch —
coarser than raw WindowSizeClass on purpose:

- **COMPACT** (width Compact — phones): stacked layouts.
- **EXPANDED** (everything wider — tablets, landscape): side-by-side zones.

**Do not introduce a third bucket.** Medium behaving like Expanded is the decision.

POS layouts must **not** reflow between Medium and Expanded — cashier muscle memory. The
POS is the one screen with two distinct layouts:

- **Phone POS**: product grid + cart panel + checkout bottom sheet.
- **Tablet POS** (`PosTabletLayout`): three fixed zones that never reflow — nav sidebar
  (owned by the app frame), product area (search, category chips, grid), and a **240dp sale
  ticket** with an **always-visible number pad**, charge button, and **cash-change calculation**.

The tablet sale ticket is a **three-phase state machine** — `SALE → TENDER → DONE` — and the
three zones **do not reflow between phases**. Only the ticket's middle band changes:

| Phase | Ticket shows | Charge does |
|---|---|---|
| `SALE` | lines, quantity hint, number pad | → `TENDER` |
| `TENDER` | lines, payment method, cash/change, number pad | commits the sale → `DONE` |
| `DONE` | lines, success banner in the product area | clears → `SALE` |

**The number pad has one layout and two destinations** — the quantity buffer in `SALE`, the
cash buffer in `TENDER`. It never moves, resizes, or relabels. This is the muscle memory
rule applied within a screen rather than across device sizes.

**Quantity editing is never bound to Charge.** A pending quantity is applied by its own
control. A button that sometimes edits a line and sometimes takes money is the kind of
overloading § Copy and voice already forbids at the label level.

Card and mobile-money tenders **skip the cash/change pair** — there is no change to give,
and showing a zeroed calculator implies otherwise.

**Detail surfaces:** COMPACT uses a bottom sheet; EXPANDED uses a right-hand side panel
pinned to the sheet edge. Same composable, two containers. Do not write two detail screens.

---

## Component library (`ui/components/`)

These are the canonical building blocks. **Future features must reuse them — never create
one-off buttons, cards, chips, badges, or empty states.**

| Component | Purpose | Status |
|---|---|---|
| `ShopMateButton` | Standard buttons | built |
| `ShopMateCard` | Card container | built |
| `ShopMateChip` | Filter / choice chips | built |
| `ShopMateSearchBar` | Search input | built |
| `ShopMateTextField` | Form text input | built |
| `QuantityControl` | +/− stepper | built |
| `StatusBadge` | Status pills (stock level, sync state) | built |
| `CategoryDot` | Category colour dot | built |
| `EmptyState` | Empty-list placeholder | built |
| `ShimmerLoading` | Loading shimmer | built |
| `AnimatedCounter` | Animated number transitions | built |
| `TrendIndicator` | Up/down trend arrow + colour | built |
| `SuccessBanner` | Checkout-Neon success feedback | built |
| `ConstrainedContent` | Max-width wrapper for wide screens | built |
| `ShopMateMark` / `ShopMateWordmark` | The looped M and the two-colour wordmark (§ Brand) | built |
| `LicenceBanner` | The plan's grace / expired line on the POS (§ Licensing) | built |
| `RunwayGauge` | Seven-day stock runway (§ Gauges) | **to build — spec below** |
| `MarginGauge` | Cost-to-price track with target notch (§ Gauges) | **to build — spec below** |
| `InsightRow` | Leading slot · name + meta · gauge · trailing value + sub | **to build — spec below** |
| `SectionHeader` | Eyebrow label + optional trailing action | **to build** |
| `LedgerRow` | Ledger line: what · when/who · signed delta · running balance | **to build — spec below** |
| `StoppedChip` | `ShopBlack` chip for out-of-stock / at-a-loss | **to build** |

`InsightRow` is the shared row used by both Stock and Products. Building it once is what
makes the two screens read as siblings — do not fork it per screen.

### Specs

**`RunwayGauge`** — 52×16dp. Seven segments, 2dp gaps, 1.5dp radius. Filled `TradeGreen`
per whole day of cover; the partial day `StockOrange`; remaining days hollow with a 1dp
`ReceiptBeige` outline. **When `ProductInsight.confident` is false, every segment renders
hollow** — the gauge must be incapable of projecting from thin data.

**`MarginGauge`** — 52×16dp, 3dp radius, `#F0EDE7` track. Cost fills from the left in
`TradeGreen`. A 2dp `ShopBlack` notch marks the store's target margin (from
`SettingsRepository`, never a constant). Cost past the notch renders the overflow in
`StockOrange`. Selling at or below cost fills the entire track `ShopBlack`.

**`InsightRow`** — 12dp vertical / 14dp horizontal padding, 12dp radius. Slots, in order:
name (14.5sp Medium, ellipsised) over meta (11.5sp `ShopBlackLight`) · gauge · trailing
value (14.5sp SemiBold) over sub (11sp). Severity tints the row surface `#FBF0E7` at the
Warning tier. Dormant items render the name in `ShopBlackLight`. **One implementation for
Stock and Products.**

**`LedgerRow`** — 9dp vertical padding. What (13sp) over when (11sp `ShopBlackLight`) ·
signed delta (13sp SemiBold, min 34dp, right-aligned) · running balance (12sp
`ShopBlackLight`, min 26dp). Restocks `TrendUp`, count corrections `StockOrange`, sales
`ShopBlack`. **Renders no actor attribution while `StockDelta.actor` is null** — the
`when` slot carries time alone until auth ships.

---

## Domain models

```kotlin
data class Product(
    val id: String,
    val name: String,
    val sku: String,
    val barcode: String?,            // primary code; all lookups go via ProductBarcode
    val categoryId: String?,
    val sellingPrice: BigDecimal,
    val costPrice: BigDecimal,
    val previousCostPrice: BigDecimal?,   // v3.0
    val costPriceChangedAt: Instant?,     // v3.0
    val sellingPriceSetAt: Instant?,      // v3.0
    val supplier: String?,                // v3.0
    val stockQuantity: Int,
    val lowStockThreshold: Int,
    val unit: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

// v3.0 — one product, many codes. Same item from two suppliers is the norm here.
data class ProductBarcode(
    val id: String,
    val productId: String,
    val code: String,
    val createdAt: Instant,
)

// v3.0 — codes scanned at the till that aren't in the catalogue yet.
// An unknown barcode NEVER blocks a sale; it lands here to be named later.
data class UnknownBarcode(
    val code: String,
    val hits: Int,
    val firstSeenAt: Instant,
    val lastSeenAt: Instant,
    val lastTill: String?,
)

data class Category(val id: String, val name: String, val colour: String)

// In-memory POS cart line — NEVER persisted to Room (lost if the app is killed).
data class CartItem(
    val product: Product,
    val quantity: Int,
    val lineTotal: BigDecimal,   // product.sellingPrice * quantity
)

data class Sale(
    val id: String,
    val receiptNumber: String,       // SM-YYYYMMDD-TTT-XXXX: day, till code, daily sequence
    val totalAmount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val cashierName: String,
    val items: List<SaleItem>,
    val createdAt: Instant,
    val syncedAt: Instant?,
)

data class SaleItem(
    val id: String,
    val productId: String,
    val productName: String,
    val categoryName: String?,
    val unitPrice: BigDecimal,
    val quantity: BigDecimal,
    val lineTotal: BigDecimal,
)

enum class PaymentMethod { CASH, ECOCASH, MOBILE_MONEY, CARD, BANK_TRANSFER }

data class StockDelta(
    val id: String,
    val productId: String,
    val delta: Int,                  // signed: + restock, − sale/removal
    val reason: DeltaReason,
    val note: String?,
    val countSessionId: String?,     // v3.0 — set when reason == COUNT
    val actor: String?,              // v3.0 — till or person; null until auth ships
    val createdAt: Instant,
    val synced: Boolean = false,
)
data class StockDeltaWithProduct(val delta: StockDelta, val productName: String)
enum class DeltaReason { SALE, RESTOCK, MANUAL_ADJUSTMENT, CORRECTION, PURCHASE, COUNT }

// v3.0 — a stocktake, so variance can be read as a pattern across sessions.
data class StockCount(
    val id: String,
    val startedAt: Instant,
    val committedAt: Instant?,
    val countedBy: String?,
    val itemCount: Int,
    val varianceCount: Int,
    val varianceValue: BigDecimal,   // signed; negative = short
)

data class SyncEvent(
    val id: String,
    val eventType: EventType,
    val payload: String,
    val createdAt: Instant,
    val synced: Boolean,
    val failed: Boolean,
    val errorMessage: String?,
)
enum class EventType { SALE, SNAPSHOT }
```

### Derived, never stored

These are computed on read. **Do not add columns for them** — a stored velocity goes stale
the moment a sale lands.

```kotlin
data class ProductInsight(
    val product: Product,
    val unitsPerDay: Double?,        // null when the trailing window is too short
    val daysOfCover: Double?,        // stockQuantity / unitsPerDay
    val daysSinceLastSale: Int?,
    val margin: Double,              // (sellingPrice − costPrice) / sellingPrice
    val marginBefore: Double?,       // using previousCostPrice, when cost has moved
    val confident: Boolean,          // false when history is thin — see rule below
)
```

**Velocity confidence rule.** `unitsPerDay` comes from a trailing 28-day window over
`sale_items`. If a product has fewer than 7 days of sales history, or no sales at all in
the window, `confident = false` and the UI **must not** project a run-out day for it. Show
"not enough history yet" or route it to Not moving. Never draw a runway from two days of data.

---

## Database

- **Room version: 6** (`AppDatabase.kt`), file `shopmate.db`.
- **Tables:** `products`, `categories`, `sales`, `sale_items`, `stock_deltas`,
  `receipt_counter`, `sync_events`, `product_barcodes`, `unknown_barcodes`, `stock_counts`.
- **Migrations are non-destructive** and live in `di/DatabaseModule.kt`: `MIGRATION_1_2`
  (adds `receipt_counter`), `MIGRATION_2_3` (adds `stock_deltas` + indices), `MIGRATION_3_4`
  (see below), `MIGRATION_4_5` (data only: gives every product whose stock its ledger can't
  explain an opening movement), `MIGRATION_5_6` (adds `sales.original_receipt_number`, so a
  sale renumbered after a receipt-number clash can still say what it was printed as). There
  is **no destructive fallback** — never add one.
- **Opening stock is a movement.** `ProductRepository.add(product, openingStock)` inserts the
  product at zero and records the opening stock as a `PURCHASE` movement noted "Opening stock",
  in one transaction. `ProductRepository.update()` never writes stock — it re-reads it, so an
  edit screen held open across a sale cannot undo the sale.

### Schema debt — MIGRATION_3_4

The Products and Stock rework cannot be honest without these. Ship this migration before
either screen.

```sql
-- pricing history + supplier
ALTER TABLE products ADD COLUMN previous_cost_price TEXT;
ALTER TABLE products ADD COLUMN cost_price_changed_at INTEGER;
ALTER TABLE products ADD COLUMN selling_price_set_at INTEGER;
ALTER TABLE products ADD COLUMN supplier TEXT;

-- one product, many barcodes
CREATE TABLE product_barcodes (
  id TEXT NOT NULL PRIMARY KEY,
  product_id TEXT NOT NULL,
  code TEXT NOT NULL,
  created_at INTEGER NOT NULL
);
CREATE UNIQUE INDEX index_product_barcodes_code ON product_barcodes(code);
CREATE INDEX index_product_barcodes_product_id ON product_barcodes(product_id);
-- backfill existing single barcodes; products.barcode stays populated for compatibility
INSERT INTO product_barcodes (id, product_id, code, created_at)
SELECT hex(randomblob(16)), id, barcode, updated_at
FROM products WHERE barcode IS NOT NULL AND barcode != '';

-- unknown codes rung up at the till
CREATE TABLE unknown_barcodes (
  code TEXT NOT NULL PRIMARY KEY,
  hits INTEGER NOT NULL,
  first_seen_at INTEGER NOT NULL,
  last_seen_at INTEGER NOT NULL,
  last_till TEXT
);

-- stocktake sessions
CREATE TABLE stock_counts (
  id TEXT NOT NULL PRIMARY KEY,
  started_at INTEGER NOT NULL,
  committed_at INTEGER,
  counted_by TEXT,
  item_count INTEGER NOT NULL,
  variance_count INTEGER NOT NULL,
  variance_value TEXT NOT NULL
);
ALTER TABLE stock_deltas ADD COLUMN count_session_id TEXT;
ALTER TABLE stock_deltas ADD COLUMN actor TEXT;
CREATE INDEX index_stock_deltas_count_session_id ON stock_deltas(count_session_id);
```

**Barcode normalisation.** UPC-A is EAN-13 with a leading zero. Normalise on write and on
lookup, or roughly half of all "unknown barcode" reports will be this and nothing else.

**`actor` is nullable and stays null until auth ships.** Do not populate it with the
hardcoded `"Cashier"` string and do not render an attribution the data cannot support. A
ledger that claims a person's name it does not know is worse than one that omits it.

### Settings

Target margin is a **store setting** in DataStore via `SettingsRepository`, not a constant.
It drives the `MarginGauge` notch and the "holds your target" price suggestion. **Default:
30%**, shipped as a DataStore value with a Settings control. `MarginGauge` has no notch
without it, so this lands with `MIGRATION_3_4`.

---

## File layout

```
com.shopmate/
├── ShopMateApp.kt               @HiltAndroidApp
├── MainActivity.kt              @AndroidEntryPoint; computes WindowSizeClass →
│                                DeviceLayout, hosts AppNavGraph
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt        (version 6; migrations in DatabaseModule)
│   │   ├── converter/Converters.kt
│   │   ├── dao/  (ProductDao, CategoryDao, SaleDao, SyncEventDao,
│   │   │          ReceiptCounterDao, StockDeltaDao, ReportDao,
│   │   │          ProductBarcodeDao, UnknownBarcodeDao, StockCountDao,
│   │   │          StockInsightDao)
│   │   └── entity/ (…Entity for each table)
│   ├── auth/
│   │   ├── AuthManager.kt              (interface; SupabaseAuthManager implements it)
│   │   └── StoreContext.kt             (active store id, chosen from the RLS-scoped
│   │                                    stores the signed-in member can see)
│   ├── mapper/ (one per model)
│   ├── remote/
│   │   ├── SupabaseClientProvider.kt   (single SupabaseClient: Postgrest + Auth)
│   │   ├── ShopMateRemote.kt           (upsert surface the sync worker calls)
│   │   ├── SessionStore.kt             (DataStore session persistence for Auth)
│   │   ├── dto/SyncDtos.kt             (row shapes, snake_case @SerialName)
│   │   └── mapper/DtoMappers.kt
│   └── repository/ (…Impl for each interface)
├── di/ (DatabaseModule, NetworkModule, RepositoryModule,
│        DataStoreModule, AuthModule, SyncModule)
├── domain/
│   ├── model/ (all models above + ReportModels + ProductInsight)
│   ├── repository/ (ProductRepository, CategoryRepository, SaleRepository,
│   │                ReportRepository, SyncRepository, SettingsRepository,
│   │                StockInsightRepository, BarcodeRepository,
│   │                StorePermissionRepository)
│   └── util/ (ReceiptNumberGenerator, BarcodeNormaliser)
├── sync/ (SalesSyncWorker, SyncScheduler, NetworkMonitor, ConflictResolver)
├── print/ (EscPosRaster — 1-bit image → ESC/POS raster bytes; no transport yet)
└── ui/
    ├── navigation/ (Screen, NavItems, NavGraph, AppSidebar,
    │                ContentHeader, AppBarViewModel)
    ├── components/  (shared library — see Component library section)
    ├── screen/
    │   ├── pos/        (PosScreen + PosScreenTablet + PosViewModel, …)
    │   ├── products/   (ProductsScreen, ProductsViewModel, AddEditProductScreen,
    │   │                RepriceSheet, BarcodeInboxScreen, ProductDetailPanel)
    │   ├── categories/ (CategoriesScreen, CategoryViewModel)
    │   ├── stock/      (StockScreen, StockViewModel, WeekAheadStrip,
    │   │                StockCountScreen, StockCountViewModel,
    │   │                CountHistorySheet, OrderListSheet, StockDetailPanel)
    │   ├── reports/    (…)
    │   ├── sales/      (…)
    │   ├── sync/       (…)
    │   ├── settings/   (…)
    │   └── splash/     (BrandSplash)
    ├── receipt/ (ReceiptContent, SharedReceiptImage, PrintedReceiptSlip, ReceiptShare)
    ├── util/ (CurrencyFormatter, RelativeTimeFormatter, DateFormatter,
    │          DeviceLayout, PaymentMethodLabel)
    └── theme/ (Color, Type, Theme)
```

---

## Architecture rules

1. ViewModels talk only to repository interfaces — never to DAOs directly.
2. Repository implementations are `@Singleton`; DAOs are scoped via `DatabaseModule`.
3. `SaleRepository.recordSale()` must be a single `@Transaction` that:
    - inserts Sale + SaleItems
    - decrements stock for each item
    - inserts a `SALE` `StockDeltaEntity` per item (negative delta, note = receipt #)
    - queues a `SyncEventEntity` with `eventType = "SALE"`
4. `ProductRepository.adjustStock()` is a single `@Transaction` that:
    - increments stock by the signed delta (**never** an absolute overwrite)
    - inserts a `StockDeltaEntity` recording the movement
    - queues a `SyncEventEntity` (eventType `"SNAPSHOT"`, payload = productId)
5. **A committed stock count is a batch of deltas, not a set of writes.**
   `StockCountRepository.commit()` is a single `@Transaction` that inserts the
   `StockCountEntity`, then for each counted item computes `counted − expected` and calls
   the same signed-delta path as rule 4 with `reason = COUNT` and the session id. Stock is
   never assigned; it is always summed.
6. **Insights are read-only and derived.** `StockInsightDao` is a read-only DAO in the
   `ReportDao` mould. Velocity, days of cover and margin are computed in the domain layer
   from its rows. No insight is ever written back to `products`.
7. All database-emitting functions return `Flow<T>` — never `LiveData`.
8. `BigDecimal` and `Instant` are stored as `TEXT` and `Long` (epoch millis) in Room via
   `Converters.kt`.
9. The `sync_events` table is the event queue — never delete rows; mark them `synced = true`
   or `failed = true`.
10. Room schema changes ship with a non-destructive `Migration` in `DatabaseModule` (current
    DB version: 6). Never use destructive migration.
11. **Barcode lookups go through `BarcodeRepository`**, which normalises the code, checks
    `product_barcodes`, and on a miss records to `unknown_barcodes` and returns null. The
    caller never blocks on a miss.
12. **Stock rights.** Selling is open to every member. Any other movement — restock, adjust,
    count, opening stock — needs `StorePermissionRepository.canManageStock`: owners and
    managers always, a cashier only when the owner has granted it for that store
    (`store_access.can_manage_stock`). `ProductRepository.adjustStock()` refuses without it,
    the UI hides those actions, and RLS on `stock_deltas` enforces the same rule on upload.
    The answer is fetched at sign-in / store pick and kept on the device for offline use.

---

## Backend & sync

**Supabase** (resolved in v3.1). Postgres + PostgREST, accessed through **supabase-kt**
(`postgrest-kt` + `auth-kt`). The Django Retrofit interface, `AuthInterceptor` and
`AuthTokenProvider` are retired — do not add new endpoints to them.

- `SUPABASE_URL` and `SUPABASE_ANON_KEY` come from `local.properties` into `BuildConfig`.
  **Only the anon key ships in the app**; every table is protected by RLS. No service-role
  key on a device, ever.
- **Server shape:** `orgs` → `memberships` (user × org × role) → `stores`. Every synced row
  carries `store_id`; RLS policies allow read/write only where the caller has a membership
  in that store's org. **RLS is the only security boundary** — the device never relies on a
  query filter for access. The device **may narrow** a read to the active store (RLS scopes
  to the org, the till works for one store); it **never widens** one. Anything pulled back
  is checked against the active store again before it touches Room.
- `SupabaseClientProvider` owns one `SupabaseClient`; `SessionStore` persists the session in
  DataStore so a shop that opens offline stays signed in. `StoreContext` picks the active
  store from the stores the member's RLS policy returns at sign-in (an owner with none is
  sent to onboarding), never from a hand-entered or locally generated id.
- **Room stays the source of truth.** Nothing on screen ever reads from Supabase directly;
  sync is one-way out of the device for sales, and snapshots in for catalogue reconciliation.
- `sync/SalesSyncWorker` (WorkManager) drains the `sync_events` queue when `NetworkMonitor`
  reports connectivity, writing through `ShopMateRemote` as **idempotent upserts keyed on the
  client-generated row id** — a retried sale must not double-post. `SyncScheduler` enqueues
  periodic + on-connect work; `ConflictResolver` handles clashes, and on a `401` the worker
  stops draining and surfaces re-authentication rather than marking events failed.
- **No Realtime subscriptions.** A till on a thin connection does not hold a socket open.
- **WorkManager's 15-minute periodic minimum** is why sync responsiveness relies on an
  immediate `SyncWorker` enqueue plus `NetworkMonitor`, not on the periodic schedule. A sync
  is enqueued on every one of these, so a cashier never needs "Sync now": checkout
  (`SalesSyncWorker`), the network returning (`NetworkMonitor`), and the till becoming able
  to upload again — sign-in, re-sign-in, store picked (`SessionSyncTrigger`).
- **Receipt numbers are unique per store on the server** (`sales_store_id_receipt_number_key`),
  and each till counts its own receipts from 0001 every day. So every number carries the
  till's code — `SM-20261022-K7Q-0001` — generated once per install by `TillCode` (three
  characters, no 0/O or 1/I). A reinstall gets a new code, so its fresh 0001 can't collide.
- **A refused sale batch is retried one sale at a time**, so one bad sale can't hold back the
  rest. A sale refused because its number is already taken (issued before numbers carried a
  till code) is renumbered from this till by `SaleRepository.renumber()` — same date, ledger
  notes follow — and sent once more. Anything else refused is marked failed.
- **A refused upload is never hidden.** The header shows "N not sent" in `ErrorRed` ahead of
  "N waiting", and the Sync screen says what happened and offers "Send again". "Up to date"
  means nothing is waiting **and** nothing was refused.
- **Stock movements upload as two requests**: SALE movements, then everything else. RLS
  lets any member insert SALE movements but only owners and managers insert the rest, and
  one INSERT is all-or-nothing — a single batch would let one cashier restock hold back
  every sale movement.
- Only two payload types leave the device: **denormalised sale records** and **periodic
  store snapshots**. Everything else stays local.

### Release build

- **R8 full mode** with resource shrinking. `app/proguard-rules.pro` holds only ShopMate's own
  rules — kotlinx.serialization, Hilt, Room, WorkManager and Ktor ship theirs. Every rule
  added there carries a comment naming the crash or R8 error it answers.
- **Signing** comes from `local.properties`: `RELEASE_STORE_FILE` (relative to the project
  root), `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Never committed.
- **Versioning** is `shopmate.versionCode` / `shopmate.versionName` in `gradle.properties`;
  CI may override with `-P`. `versionCode` only ever goes up.
- **`verifyReleaseConfig`** fails any release build missing the Supabase keys or signing —
  a till must never ship pointing at the placeholder project. Debug builds are unaffected.

---

## Tenancy, staff and licensing

Decided 2026-10-23. Built in the order below; each step ships on its own.

### One store per device

The organisation is the tenant: `orgs` → `memberships` (role) → `stores`. **A till serves one store
at a time.** Room has no `store_id` column and never gets one — every local row belongs to the
active store.

- **Switching store** sends everything first. It is refused while anything is waiting or not
  sent (the till says so and offers Sync), and offline. Then it clears the local database, sets
  the new active store, and loads that store's catalogue. Nothing local is ever discarded
  unsent.
- **A fresh till loads its store's catalogue** (`CatalogueLoader`): every server product for the
  active store that the till doesn't have is inserted, its server stock recorded as an
  "Opening stock" movement marked already synced — the server holds that stock, so it never
  uploads again. This is what lets a new staff phone sell on day one.
- **The owner adds stores** (`create_store` RPC, checked against the licence's store limit).

### Staff — email for the owner, PINs for everyone else

Amended 2026-10-23 (replaces invite codes, which needed staff to confirm an email address first).

- **Only the owner has an email account.** The owner signs in once to set up a till; the till
  stays signed in as the store. Managers and cashiers never create an account.
- **The owner adds staff** in Settings: name, role (`manager` or `cashier`), a 4-digit PIN, and
  for a cashier whether they can move stock. Staff belong to one store. Online only — the staff
  list lives on the server (`staff` table) and every till of that store pulls it on sync.
- **Staff unlock the till with their PIN** on the "Who's selling?" screen: tap your name, enter
  your PIN. Works offline. The till locks when the app starts and when someone taps **Switch
  cashier**. The owner has a PIN too ("Your till PIN"), set before adding anyone else.
- A till with no staff on its store has no lock — a one-person shop never sees it.
- **PINs are never stored in the clear**: PBKDF2-SHA256 with a per-PIN salt, hashed on the
  device that sets them. A 4-digit PIN is a till lock, not a password; it guards the till, not
  the account, and only members of the store can read the hashes.
- **Every sale records who rang it up** (`cashier_name`, `staff_id`), and every stock movement
  its actor. This is the attribution that waited for auth.
- **Rights follow the person on the till.** Cashier: sell; move stock only with the grant.
  Manager: sell and move stock, sees Settings but not Staff, Sign out or Reset. Owner: everything.
  Uploads still go out under the owner's session — the server enforces store and plan, the till
  enforces the person.
- The plan's staff limit counts PIN staff (not the owner).

### Licensing — plans with limits

- One **licence per organisation**: plan (`trial`, `basic`, `pro`), `expires_at`, `max_stores`,
  `max_staff`. Set and extended by ShopMate from the Supabase dashboard; no payment integration
  yet. A new org starts on a 30-day trial.
- The device reads `current_licence()` at sign-in, store pick and every sync, and **keeps the
  last answer** for offline use. It never decides from the device clock alone that a licence it
  hasn't seen is valid.
- **States:** active → *ending* (7 days before expiry: a calm banner on the owner's Settings and
  Reports, never the cashier's POS) → *grace* (7 days after expiry: banner on POS too) →
  *expired* (new sales are refused at Charge with the reason). **Sales already taken always
  upload** — the server accepts queued sales dated before expiry plus grace. Nothing is lost.
- Limits are enforced on the server (`create_store`, `create_invite`); the app shows the limit
  in words before the owner hits it.

---

## Build status

**Supabase cutover: done (app side).** supabase-kt Auth + PostgREST only (no Realtime),
session in DataStore via `SessionStore`, all uploads through `ShopMateRemote`, a `401`
stops the drain and flags re-authentication. Retrofit, Moshi, OkHttp interceptors and
`BASE_URL` are removed.

**Built (Phases 1–7):** Room/Hilt/Compose scaffold · Products + Categories CRUD · POS with
cart, search, checkout, receipt numbering · Stock levels, filters, adjustment sheet,
movement history · Reports (Vico) · Sync engine · Settings. Beyond plan: sidebar app frame,
adaptive DeviceLayout, tablet POS, Sales History, shared component library, Inter bundled.

**Tenancy, staff and licensing (2026-10-23):** app side built — store switching with its
guard, `CatalogueLoader`, join-a-shop onboarding, Settings › Plan / Add a store / Staff with
invite codes, licence states with the POS banner and the expired-plan block. Server side is
`supabase/migrations/20261023090000_tenancy_staff_licensing.sql`; until it is applied the staff
list and plan don't load and nothing is blocked. No payment integration: licences are set from
the dashboard.

**Not built yet**, in dependency order:
- `MIGRATION_3_4` and everything downstream (§ Schema debt) — **blocks the three below**
- Target margin setting, default 30% (§ Settings) — blocks `MarginGauge`
- `RunwayGauge`, `MarginGauge`, `InsightRow`, `LedgerRow`, `SectionHeader`, `StoppedChip`
- `ScanSource` abstraction (HID first, CameraX + ML Kit behind it) — blocks the scanner
  overlay and the unknown-barcode path
- Tablet POS tender phases + payment method in the ticket (§ Adaptive layouts)
- `AppSidebar` COMPACT variant (§ Navigation)
- `ChargeButton` recoloured to `CheckoutNeon` (§ Colours)
- Stock rework: week-ahead strip, severity sections, count sessions, order list
- Products rework: margin health, reprice, multi-barcode, unknown-barcode inbox
- Owner digest — designed, unrouted, see open question 2
- End-to-end sync check against a real Supabase project with RLS on (app side compiles
  and builds; not yet run against the server)
- **Attribution** — the commit after the cutover: `StockDelta.actor`, cashier names on
  sales (still the hardcoded `"Cashier"`), and the count-history pattern card
- Final polish pass

---

## Open questions — resolve before the next phase

None. Add new ones here rather than deciding them in a composable.

### Resolved after v3.1

- **Who moves stock other than by selling: owners and managers; a cashier only with the
  owner's grant**, per store. See architecture rule 12.

- **Store filter on the catalogue pull: the rule is amended, RLS is unchanged.** RLS scopes
  to the org, so a member of a multi-store org can read every store's products. Narrowing
  RLS to one store would need the active store in the JWT (a custom access-token hook, and
  a token refresh — impossible offline — on every store switch) or per-store memberships.
  Instead the device narrows its own reads to the active store, and `ConflictResolver`
  skips any pulled row whose `store_id` is not the active store, so a wrong or missing
  filter cannot reprice a till. See § Backend & sync.

### Resolved in v3.1

- **Backend: Supabase.** Postgres + PostgREST + RLS over org → memberships → stores, via
  supabase-kt. § Backend & sync is rewritten accordingly and the Django Retrofit layer is
  retired. Offline-first is unchanged: Room remains the source of truth and the
  `sync_events` queue is still the only way data leaves the device.
- **Auth timing: with the Supabase cutover, as one piece of work.** Supabase Auth is what
  provides the session, and the membership row is what provides the store id — so RLS
  cannot work before sign-in exists, and the cutover cannot ship without it. That makes the
  cutover the earliest honest point for attribution, and `StockDelta.actor`, cashier names
  on sales and the count-history pattern card all ship in the commit after it. Until then
  they ship with no attribution rather than a placeholder.
- **Owner dashboard: beside this app, later.** The daily digest is designed but not routed
  and not in the sidebar. A remote owner opening a till app to read a summary is a
  different session shape from a cashier's, and a sixth destination for a user who isn't
  holding the device weakens the frame for the one who is. Revisit once auth exists — the
  digest's best content ("three short counts on the morning shift") is blocked on
  attribution anyway.
- **Target margin default: 30%**, in `SettingsRepository` (§ Settings).