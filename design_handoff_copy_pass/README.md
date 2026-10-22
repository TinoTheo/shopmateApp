# Handoff: ShopMate copy and states pass

## Overview
This pass fixes copy and state handling in five areas of the ShopMate Android app:
1. Sync history rows and their failure states
2. Counts without plurals
3. Status-bar icons on sign-in and onboarding
4. Run-out language on Stock
5. Reports copy and chart colours

**Layouts don't change.** Only strings, state logic and a few colour treatments change.

## About the design files
`ShopMate Copy Pass.dc.html` is a **design reference built in HTML**. It is not production code. Rebuild it in the existing codebase (Jetpack Compose, `ui/theme/Color.kt`, `Type.kt`, `ui/components/`). Open the file in a browser. Each mock has an ID (`3a`–`3q`).

`copy/strings_copy_pass.xml` holds every final string, including `<plurals>`. Merge it into `res/values/strings.xml`. Where a key already exists, replace its value. Delete the old single-form strings it replaces.

## Fidelity
- **Copy is final.** Use it word for word, including the typographic apostrophes (’) and the minus sign (−) in stock deltas.
- **Visual treatments are high-fidelity** where the README calls them out below.
- Mocks of the sync, sign-in and tablet screens were built from the written brief, not from screenshots. **Where the shipping screen's layout differs from the mock, keep the shipping layout and apply the copy and states only.**

## Rules (from CLAUDE.md, unchanged)
- **Name things by what the person controls**, not by how the system works.
- **Failures say what happened and what to do.** No apologies, nothing vague.
- **Offline is a state, not an error.** No error styling.
- **Sentence case everywhere.** Buttons say what happens.
- **Never show a number as certain when it isn't.**
- **Colour:**
  - `ErrorRed` `#CC4436`: real failures only.
  - `StockOrange` `#E07B39`: "will hurt soon".
  - `TradeGreen` / `CheckoutNeon`: the only brand colours.
- **Type:** Inter only; `tnum` on every number.

---

## 1. Sync history (mocks 3b–3f)

### Row titles
Built from what was uploaded:

| Upload | Title | Meta |
|---|---|---|
| Sale | `Sale · {amount}` | `{receipt} · {HH:mm}` |
| Store snapshot | `Product list` | `Prices and stock on this till · {HH:mm}` |
| Restock | `Restock · {product} +{qty}` | |
| Correction | `Stock correction · {product} −{qty}` | |
| Stock count | `Stock count · {n} item(s)` (plural) | |
| Price change | `New price · {product} {price}` | |

### Row status
`Waiting` · `Sending…` · `Sent` (TradeGreen dot) · `Paused` · `Not sent` (ErrorRed dot and text, 500 weight).

### Map server errors to a reason
Add a `SyncFailureReason` sealed class, set by the sync worker. **Never show `error.message` in the main UI.**

| Server response | Reason | Row state | Copy keys |
|---|---|---|---|
| 409 on `sales_store_id_receipt_number_key` | `ReceiptNumberTaken` | Renumbering: `Waiting`. Once resent: `Sent` | `sync_reason_renumbering` → `sync_reason_renumbered_meta` + `sync_reason_renumbered` |
| 403 / RLS | `NotAllowed(kind)` (kind = stock, price or sale; from the upload type) | `Not sent` | `sync_reason_forbidden_*` + `sync_todo_forbidden_*` |
| 401 / session expired | `SignedOut` | `Paused` | `sync_reason_signed_out_meta`. This must agree with the "Sending is paused" card. |
| 5xx, timeout, no network | not a failure | `Waiting` | `sync_reason_no_answer`. Keep retrying with backoff. |
| Any other 4xx | `Other` | `Not sent` | `sync_reason_other_*` + `sync_todo_other` |

After the automatic renumber, the row keeps a second meta line: the old number was used on another till, and the customer's paper receipt still shows it.

### Expanded refused row (3e)
Tapping a `Not sent` row expands it. From top to bottom:
1. Reason sentence, 13.5sp `ShopBlack`.
2. "What to do" (13sp SemiBold), followed by the action sentence (13.5sp `ShopBlackMid`).
3. Buttons: `Send again` (outlined, TradeGreen, 40dp tall), then a `Details` text toggle with a chevron.

`Details` is **collapsed by default**. When open it shows a box (`ShopOffWhite` fill, 1dp `ReceiptBeige` border, 10dp radius) containing:
- label "For the owner or support", 11sp `ShopBlackLight`
- the raw server text, 12sp `ShopBlackMid`, Inter (not monospace)
- `{kind} · {date}, {HH:mm} · Till {n}`
- `Copy details`, which shows the snackbar "Details copied"

`Send again` goes Sending… → Sent. If it's refused again, show the snackbar "Refused again. Open Details to see why."

### Sync status card (top of the screen)
Show exactly one of these, in this priority order:

| Priority | State | Title | Body | Action |
|---|---|---|---|---|
| 1 | Signed out | "Sending is paused" | `sync_card_paused_body` (plural) | `Sign in` (filled) |
| 2 | Any not sent | ErrorRed dot + `sync_card_not_sent_title` | `sync_card_not_sent_body`. If uploads are also waiting, add a hairline and a "3 waiting to send … Send now" line | `See why` expands and scrolls to the first refused row |
| 3 | Offline + waiting | `sync_card_waiting_title` | `sync_card_offline_body`, then a hollow dot and "Offline since {HH:mm}" | none |
| 4 | Online + waiting | `sync_card_waiting_title` | breakdown, e.g. "2 sales and 1 product list", from the `sync_kind_*` plurals | `Send now` (filled) → Sending… → Sent |
| 5 | Nothing waiting | "Everything is sent" | "Last sent at {HH:mm}" | none |

**Fix the endless spinner:**
- `Send now` is hidden while `NetworkMonitor` reports offline.
- It never waits for a connection with a spinner.
- If the network drops mid-send, fall back to state 3.

**Footer:** "Connected to: https://…supabase.co" becomes `sync_footer` ("Sends to the {store} store account"). Move the URL to Settings › About.

### Tablet (3f)
Two columns:
- **Left (flexible):** the status card, then the history list. The selected row has a white fill, a 1dp border and a 12dp radius.
- **Right (440dp):** the selected row's expanded content, in a white card.

---

## 2. Plurals
Every count goes through `pluralStringResource`. **Keep the `one`/`other` pair even where English is identical**, so later locales (Shona, Ndebele, Zulu, Afrikaans) can differ.

| Where | Key | Zero case |
|---|---|---|
| Header sync status | `header_status_waiting`, `header_status_not_sent`, joined with `header_status_join` | Drop any segment whose count is 0. If both are 0, show `header_status_up_to_date`. If offline or paused, prefix it: "Offline · 3 waiting". |
| Sync card | `sync_card_waiting_title`, `sync_kind_*`, `sync_card_not_sent_*` | The card switches state (see table above) |
| POS cart / checkout | `pos_items` | `pos_cart_empty` |
| Sales history row | `pos_items` | not possible |
| Stock filters | `stock_filter_*` + `stock_filter_with_count` | Show the label without a count; the chip stays tappable |
| Stock row meta | `runout_*` (see § 4) | |
| Products banner | `products_unnamed_codes` | Hide the banner |
| Unknown codes card | `barcode_scanned` | not possible |
| Reports | `reports_sold_*`, `reports_stat_*` | `reports_empty_*` |

Counts in the POS item count are quantities (2 loaves and 1 Coke = 3 items).

### Header status visuals (3g)
- **Up to date:** TradeGreen dot.
- **Waiting:** grey (`ShopBlackLight`) dot.
- **Offline or paused:** hollow grey dot.
- **Not sent:** ErrorRed dot and text at 500 weight. This is the only red in the header.

---

## 3. Sign-in and onboarding status bar (3h, 3i)
**Decision:** give both screens a Trade Green top band, so the app-wide white status-bar icons are correct. **Don't** switch icon colour per screen.

- **Band:**
  - Sign in: 210dp, including the 44dp status bar.
  - Onboarding: 170dp.
  - Stacked logo (`stacked-40`) 132dp wide, 28dp from the left, vertically centred, placed **directly on the green**. Drop the green logo tile, because the band replaces it.
- **Sheet:** `ShopWhite`, 24dp top-start radius, padding 32dp top and 24dp sides. Internal layout and keypad are unchanged.
- Copy keys: `signin_*`, `onboard_*`.

---

## 4. Run-out language (3j–3m)
### One source of truth
Add `RunOut` to `ProductInsight`, computed once. The list row, the detail sheet, the week-ahead strip and the caption **all read this value**. None of them recompute or reformat it.

```kotlin
sealed interface RunOut {
    data class Out(val since: LocalDate) : RunOut
    data object Today : RunOut
    data object Tomorrow : RunOut
    data class OnDay(val day: DayOfWeek) : RunOut      // 2–6 calendar days
    data object OverAWeek : RunOut                       // 7–13
    data class OverWeeks(val weeks: Int) : RunOut        // 14–27, weeks = days / 7
    data object OverAMonth : RunOut                      // 28+
    data class NoHistory(val soldSinceAdded: Int, val addedOn: LocalDate) : RunOut
    data class Dormant(val days: Int) : RunOut
}
```

**How to compute it:**
1. `runOutAt = now + stock / velocityPerDay`.
2. `days = ChronoUnit.DAYS.between(today, runOutAt.toLocalDate())`.
3. Compare **calendar dates**. Don't round or truncate a count of days. Rounding versus truncating is the cause of the bug where the list said "1 days left" and the detail said "0 days left".

### Where each value is used
| Variant | List row meta | Detail | Strip |
|---|---|---|---|
| Out | `runout_out` + black "Out" chip | `runout_out_since` | not shown; counted in the caption |
| Today | `runout_today` | same, then `runout_basis` | today column |
| Tomorrow / OnDay | `runout_tomorrow` / `runout_day` (full day name) | same, then `runout_basis` | that column |
| OverAWeek / OverWeeks / OverAMonth | `runout_week` / `runout_weeks` / `runout_month` | same | not shown |
| NoHistory | `runout_no_history` | same, then `runout_no_history_basis` | not shown |
| Dormant | `runout_dormant` | same | not shown |

`runout_basis` is "Selling about {n} a day this week". Round n to a whole number, or to one decimal place when it's below 1.

### Week-ahead strip and caption
- The first column is labelled **Today** (Medium weight) instead of the day name.
- Caption: `stock_week_caption_first` (first day = `stock_week_when_today` / `_tomorrow` / `_day`), followed by `stock_week_caption_out` if any items are out. If nothing runs out, use `stock_week_caption_none`.

### Section headers
Use `stock_section_*`. "Runs out this week" becomes "Runs out in the next 7 days", because the strip runs 7 days from today. "Stopped" becomes "Out of stock", to match the row.

Gauge behaviour is unchanged.

---

## 5. Reports (3n–3q)
### Copy by period
| Period | Subtitle | Hero label | Trend |
|---|---|---|---|
| Today | "Thursday 22 October, so far" | `reports_hero_today` | `reports_trend_*_today` |
| This week | "Monday 19 to Thursday 22 October, so far" | `reports_hero_week` | `reports_trend_*_week` (day = today's name) |
| This month | "1 to 22 October, so far" | `reports_hero_month` | `reports_trend_*_month` (prev month, ordinal day) |
| Last month | "1 to 30 September" | `reports_hero_month` | `reports_trend_*_closed_month` |
| Pick dates | "3 to 9 October" | `reports_hero_range` | `reports_trend_*_range` (plural on the day count) |

- **Greeting:** `reports_greeting_*`, chosen by the hour (before 12 = morning, 12–17 = afternoon, after that = evening). **Don't include a name.**
- **Chips:** `reports_chip_*`. "Custom" becomes "Pick dates".
- **Trend edge cases:**
  - Change within ±1%: `reports_trend_level`.
  - No earlier data: `reports_trend_none`.
  - Trend colours: the existing `TrendUp` / `TrendDown` tokens. The mock's hex values are placeholders.

### Comparisons need new queries
For periods that aren't finished, **compare like with like**:
- **Today:** yesterday's sales up to the current time of day.
- **This week:** last week, Monday up to the same weekday and time.
- **This month:** last month, day 1 up to the same day of the month (clamp to that month's length).
- **Pick dates:** the same number of days immediately before the chosen range.

Add these queries to `ReportRepository`. Never compare a partial period against a full one.

### Other strings
- **Top seller:** `reports_top_title` + `reports_sold_*`.
- **Hero stats:** `reports_stat_*`. "average" becomes "average sale".
- **Sections:** `reports_section_payments`, `reports_section_best`.
- **Empty states:** `reports_empty_*`. Open periods say "yet"; closed periods don't.

### Chart (Vico)
- Bars: `TradeGreen`, top corners rounded 3dp.
- **The open hour or day is TradeGreen at 38% alpha**, with the caption `reports_chart_partial_hour` / `_day` below the chart (11.5sp `ShopBlackLight`).
- Future days in the week or month: dashed `ReceiptBeige` outline stubs, 16dp tall.
- Gridlines `#EEEAE5`, 1dp. Add this to `Color.kt` as `ChartGrid`, or use `ReceiptBeige` at 40%.
- Axis line `ReceiptBeige`. Axis labels 10.5sp `ShopBlackLight` with `tnum`.
- **Payments bar:** Cash = TradeGreen, mobile money = TradeGreen at 55%, Card = ReceiptBeige. No other colours.

### Tablet (3q)
- **Left column (400dp):** greeting, chips (wrapping), hero (44sp amount), top seller.
- **Right column:** chart (200dp tall), then Payments and Best sellers side by side.

---

## Open items: confirm before or during the build
1. **Unit plurals.** "7 loaf" comes from product data. Decide whether products store a plural unit, or whether to render it as "7 · loaf".
2. **No-history threshold.** How many days of sales before a projection is shown? The copy doesn't state a number on purpose; keep the existing threshold.
3. **Owner variant of the 403 message.** Hold it until auth and roles ship. For now every user sees the cashier wording.
4. **Where the Supabase URL goes.** The proposal is Settings › About.

## Files
- `ShopMate Copy Pass.dc.html`: all mocks and the full copy sheet (3a), with the rule behind each change.
- `copy/strings_copy_pass.xml`: final strings and plurals.
- `assets/`: `mark.png`, `stacked-40.png` (logo references for the sign-in band).
- `support.js`: lets the HTML open locally. Not part of the build.

## Suggested Claude Code prompt
> Read `CLAUDE.md`, then `design_handoff_copy_pass/README.md`. Work in this order:
> 1. Merge `strings_copy_pass.xml` into `strings.xml` and delete the old single-form strings.
> 2. Add `RunOut` to `ProductInsight` and switch the Stock list, detail, strip and caption to it.
> 3. Add `SyncFailureReason`, the sync card state priority and the expanded row with Details. Hide Send now while offline.
> 4. Change the ContentHeader sync status to use plurals.
> 5. Add the Trade Green band to sign-in and onboarding.
> 6. Reports: period-aware copy, the like-for-like comparison queries and the chart tokens.
>
> Don't change layouts. Flag any string that has no key in the XML instead of writing new copy.
