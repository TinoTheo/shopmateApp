# CLAUDE.md — proposed v3.1 amendments

**Draft · 2026-09-02.** These are *proposed* edits to `ShopMate Android — CLAUDE.md`,
written against v3.0. Nothing here is ratified. Diff each section against the current
file, decide, and merge the ones you accept in a single commit — the file's own rule is
that a decision gets amended here first, never silently overridden in a composable.

Four of these resolve contradictions the design work exposed. Two are new component
specs. One closes an open question.

---

## 1. § Navigation & app frame — the phone sidebar

**Current text says:** "A full-height Trade Green sidebar (`AppSidebar`, 84dp) on the
left, on **all** device sizes".

**Problem.** On a 390dp phone, 84dp is 22% of the width. The content sheet gets 306dp,
and the POS product grid — `GridCells.Adaptive(120.dp)` with 16dp padding and 12dp gaps —
fits exactly two columns with 21dp of slack. It works, but every screen pays 84dp for a
component the cashier looks at maybe twice a shift.

**Proposed replacement:**

> A **full-height Trade Green sidebar** (`AppSidebar`) on the left, on **all** device
> sizes. Two width variants, chosen by `DeviceLayout` — never a third:
>
> - **COMPACT — 64dp, icons only.** Destination labels appear on **long-press** as a
>   `ShopBlack` tooltip anchored to the right of the pressed item. Selection is carried
>   entirely by the Checkout Neon pill.
> - **EXPANDED — 84dp, icon over label.** Labels always visible, as built.
>
> Both variants keep the same structure: Checkout Neon brand mark on top, destinations in
> a scrollable middle column, **Settings pinned at the bottom**. Selection animates with
> `animateColorAsState`. Unselected icons are white at 72%.
>
> The rail is the only part of the frame that changes with device size. It is still a
> sidebar, still green, still full height, and there is still **no bottom navigation bar
> on any device size**.

**Reasoning.** The frame's identity is the green column and the Neon pill, not the labels.
Dropping the labels on COMPACT returns 20dp to every screen and costs a cashier nothing —
they learn five icons in a day and long-press covers the rest. Rejecting this is
defensible; if you do, the design falls back to 84dp everywhere and the POS grid keeps two
columns regardless.

---

## 2. § Adaptive layouts — tablet POS gains a tender phase

**Add after the `PosTabletLayout` paragraph:**

> The tablet sale ticket is a **three-phase state machine** — `SALE → TENDER → DONE` —
> and the three zones **do not reflow between phases**. Only the ticket's middle band
> changes:
>
> | Phase | Ticket shows | Charge does |
> |---|---|---|
> | `SALE` | lines, quantity hint, number pad | → `TENDER` |
> | `TENDER` | lines, payment method, cash/change, number pad | commits the sale → `DONE` |
> | `DONE` | lines, success banner in the product area | clears → `SALE` |
>
> **The number pad has one layout and two destinations** — the quantity buffer in `SALE`,
> the cash buffer in `TENDER`. It never moves, resizes, or relabels. This is the muscle
> memory rule applied within a screen rather than across device sizes.
>
> **Quantity editing is never bound to Charge.** A pending quantity is applied by its own
> control. A button that sometimes edits a line and sometimes takes money is the kind of
> overloading § Copy and voice already forbids at the label level.
>
> Card and mobile-money tenders **skip the cash/change pair** — there is no change to
> give, and showing a zeroed calculator implies otherwise.

---

## 3. § Colours — the Checkout Neon rule, POS row

**Current table entry:** `POS | the Charge button; the queue tick as it uploads; the
confirmation banner edge`.

`ChargeButton.kt` currently fills `TradeGreen`. The rule says Neon. **The code is wrong,
not the rule** — Charge is the definitive irreversible commit in the app, and if it
doesn't get Neon nothing does.

**Proposed addition to the rule:**

> Where a built composable contradicts this table, the table wins and the composable is
> corrected. `ChargeButton` fills `CheckoutNeon`; its geometry (14dp radius, 18dp padding,
> 17sp Medium, 38% alpha disabled) is unchanged.
>
> **Neon does not repeat within one commit path.** The tablet Charge button and the
> success banner are the same commit at two moments — the banner's 4dp edge is Neon, its
> fill is Neon-subtle, and nothing else in the ticket competes.

---

## 4. § Component library — specs for four of the "to build" entries

The table lists these as to-build with a one-line purpose. Sizes below make them
buildable without re-deriving them per screen.

> **`RunwayGauge`** — 52×16dp. Seven segments, 2dp gaps, 1.5dp radius. Filled
> `TradeGreen` per whole day of cover; the partial day `StockOrange`; remaining days
> hollow with a 1dp `ReceiptBeige` outline. **When `ProductInsight.confident` is false,
> every segment renders hollow** — the gauge must be incapable of projecting from thin
> data.
>
> **`MarginGauge`** — 52×16dp, 3dp radius, `#F0EDE7` track. Cost fills from the left in
> `TradeGreen`. A 2dp `ShopBlack` notch marks the store's target margin (from
> `SettingsRepository`, never a constant). Cost past the notch renders the overflow in
> `StockOrange`. Selling at or below cost fills the entire track `ShopBlack`.
>
> **`InsightRow`** — 12dp vertical / 14dp horizontal padding, 12dp radius. Slots, in
> order: name (14.5sp Medium, ellipsised) over meta (11.5sp `ShopBlackLight`) · gauge ·
> trailing value (14.5sp SemiBold) over sub (11sp). Severity tints the row surface
> `#FBF0E7` at the Warning tier. Dormant items render the name in `ShopBlackLight`.
> **One implementation for Stock and Products** — the shared row is what makes them read
> as siblings.
>
> **`LedgerRow`** — 9dp vertical padding. What (13sp) over when (11sp `ShopBlackLight`) ·
> signed delta (13sp SemiBold, min 34dp, right-aligned) · running balance (12sp
> `ShopBlackLight`, min 26dp). Restocks `TrendUp`, count corrections `StockOrange`,
> sales `ShopBlack`. **Renders no actor attribution while `StockDelta.actor` is null** —
> the `when` slot carries time alone until auth ships.

---

## 5. § Copy and voice — one addition

> **Offline reassurance is placed, not just worded.** The "held on this device" line
> belongs on the surface where the money was taken — the receipt, the queue — and nowhere
> else. Repeating it on every screen turns a calm fact into a nag, which is the same
> failure as error styling by a different route.

---

## 6. § Open questions — proposed resolutions

**Question 2 — does the owner dashboard live in this app or beside it?**

> **Proposed: beside it, later.** The daily digest is designed and included in the
> handoff, but it is not routed and not in the sidebar. A remote owner opening a till app
> to read a summary is a different session shape from a cashier's, and adding a sixth
> destination for a user who isn't holding the device weakens the frame for the one who
> is. Revisit once auth exists — the digest's best content ("three short counts on the
> morning shift") is blocked on attribution anyway.

**Question 3 — target margin default.**

> **Proposed: 30%.** Ships as a `SettingsRepository` value in DataStore with a Settings
> control, per § Settings. `MarginGauge` has no notch without it, so this blocks the
> Products rework and should land with `MIGRATION_3_4`.

**Questions 1 (Django vs Supabase) and 4 (auth timing)** are untouched by this design work
and stay open. Both are load-bearing: question 4 in particular gates the ledger's actor
column, cashier attribution on sales, and the count-history pattern card.

---

## 7. § Build status — proposed update

Move to **Not built yet**, in dependency order:

> - `MIGRATION_3_4` and everything downstream (§ Schema debt) — **blocks 2, 3, 4 below**
> - Target margin setting (§ Settings) — blocks `MarginGauge`
> - `RunwayGauge`, `MarginGauge`, `InsightRow`, `LedgerRow`, `SectionHeader`, `StoppedChip`
> - `ScanSource` abstraction (HID first, CameraX + ML Kit behind it) — blocks the scanner
>   overlay and the unknown-barcode path
> - Tablet POS tender phases + payment method in the ticket
> - `AppSidebar` COMPACT variant (§ Navigation, amended)
> - Stock rework · stock count sessions · Products rework · barcode inbox
> - Owner digest — designed, unrouted, see open question 2
> - Auth / session management — still gating attribution everywhere

Unchanged: the backend server and the final polish pass.
