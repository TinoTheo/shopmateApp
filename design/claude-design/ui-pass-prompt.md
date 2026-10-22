# ShopMate — UI pass prompt for Claude Design (copy and states)

A focused follow-up to `brief.md`: five things the last rounds of testing left for the UI pass.
Paste everything under **The prompt** into Claude Design and attach:

1. These screenshots from `design/claude-design/screens/`:
   - `phone-20-sync.png`, `phone-26-sync-paused-sign-in-needed.png`, `phone-30-sync-not-sent.png`
     (Sync screen: waiting, sign-in needed, an upload refused)
   - `phone-29-pos-header-not-sent.png` (the header's sync status with a refused upload)
   - `phone-28-sign-in-current.png` (sign-in as it ships now, with the new logo)
   - `phone-13-stock.png`, `phone-14-stock-scrolled.png`, `phone-15-stock-detail-ledger.png`
   - `phone-18-reports.png`, `phone-19-reports-scrolled.png`, `phone-23-reports-week.png`
   - `tablet-09-reports.png`, `tablet-10-sync.png`
2. `CLAUDE.md` (repo root) — § Copy and voice, § Severity and § The brand promise are the rules.

---

## The prompt

You're doing a focused copy-and-states pass on **ShopMate**, an offline-first Android POS for
small shops in Zimbabwe and South Africa. The brand and layout are settled — don't redesign
screens. Five specific problems need fixing, each with the exact current text below. For each
one, give me the final copy (every variant and plural form), the visual treatment if it changes,
and a mock of the screen with the fix in place, on phone and, where it appears, tablet.

### Rules that apply to all five (from CLAUDE.md — keep them)

- **Name things by what the person controls**, never by how the system works: "3 sales held on
  this device", not "3 unsynced events".
- **Failure states say what happened and what to do**, in the app's own voice. They don't
  apologise and they're never vague.
- **Offline is a state, not an error.** Calm and specific; no error styling.
- **Sentence case everywhere.** Buttons say what happens — never Submit / OK / Confirm.
- **Never show a number as certain when it isn't.**
- **Colour:** `ErrorRed` (#CC4436) only for real failures; `StockOrange` (#E07B39) for "will hurt
  soon"; Trade Green #005B52 and Checkout Neon #99FFAA are the only brand colours. Inter only,
  tabular figures for every number.

### 1. Sync history shows raw server errors

A sync history row that failed expands (on tap) to show the error exactly as the server
returned it, for example:

> Supabase 409: duplicate key value violates unique constraint "sales_store_id_receipt_number_key"

> Supabase 403: new row violates row-level security policy for table "stock_deltas"

The row titles are system words too: **"Sale sync"** and **"Store snapshot"**.

Design:
- Plain-language row titles: say *which sale* (receipt number, amount) or what the snapshot is
  for the person ("Price list sent to your store"?), not the mechanism.
- A plain-language reason for each failure the app can actually hit, plus a fallback. The cases:
  - **Receipt number already used** (409, `receipt_number`). The app now renumbers and resends
    these by itself; the row should say so once it has ("Sent as SM-20261022-XXL-0002").
  - **Not allowed for this account** (403 / RLS): e.g. a cashier without stock rights restocked.
    What should the cashier do — ask the owner?
  - **Signed out / session expired** (401): already handled by the "Sync paused" card; the row
    should agree with it.
  - **Anything else** (other 4xx): a fallback that still says what to do.
- Whether the technical detail stays available at all (e.g. a small "Details" for the owner or
  support), and how it's set apart from the plain text.

Related, on the same screen: the **"1 update didn't reach your store"** card (screenshot
`phone-30`) and **"Send again"** button were added quickly — refine them. Also: the
**"Sync now"** button spins forever while offline (it waits for a connection), and
**"Connected to: https://…supabase.co"** at the foot is a system detail.

### 2. Counts without plurals

These strings take a number but have one form, so they read wrong at 1 (and sometimes at 0):

| Where | Current text |
|---|---|
| Header sync status | `%d waiting` · `%d not sent` |
| Sync card | `%d sales waiting to sync` (counts uploads, not only sales — a store snapshot was shown as a sale) · `%d updates didn't reach your store` |
| POS cart / checkout | `%d item(s)` · `%d items` |
| Sales history row | `3 item(s)` |
| Stock filters | `Low Stock (%d)` · `Out of Stock (%d)` — also title case |
| Stock row meta | `%d days left` → shows **"1 days left"** (see 4) · `Not sold in %d days` |
| Products | `%d codes scanned but not named` |
| Unknown codes | `%d hits · first seen …` |
| Reports | `%d sold in this stretch` |

Give me the one / other forms for each (Android `plurals`), and say where a zero case needs its
own wording ("Nothing waiting" rather than "0 waiting").

### 3. Sign-in: invisible status-bar icons

`phone-28`: the sign-in screen is light (Shop White) but the app forces **white** status-bar
icons for the green frame, so the clock, signal and battery disappear. Decide the treatment:
dark status-bar icons on this screen, or give sign-in a Trade Green top band (the brand handoff's
1j layout: lockup on green, white sheet with a 24dp top-start corner) so white icons are correct.
Keep the stacked-logo tile on Trade Green that ships now unless the layout changes. Same
question for onboarding ("Create your shop").

### 4. "1 days left" — and the run-out language around it

Stock rows say `%d days left`. At one day it reads **"1 days left"**, and for the same product
the detail sheet said **"0 days left"** while the list said **"1 days left"** (`phone-14`,
`phone-15`) — one rounds, the other truncates.

Design the run-out language as one system:
- under a day ("runs out today"?), one day, a few days, a week or more;
- CLAUDE.md prefers the day name over a count — "Runs out Thursday" beats "Low stock";
- products with too little history to project ("Not enough history yet") and dormant ones
  ("Not sold in 18 days");
- the week-ahead strip's caption ("6 items run out Friday, Monday, Tuesday and Wednesday. One is
  already out.").
The list and the detail must always say the same thing.

### 5. Reports copy

Current text (`phone-18`, `phone-19`, `phone-23`, `tablet-09`):

- Greeting: **"Good afternoon, Jipe K1."** — greets the *store name* as if it were a person.
  Subtitle "Here's how the shop's doing."
- Hero: **"Earned today"** / **"Earned this stretch"** — "stretch" is vague.
- Trend: **"Up 12% — better than yesterday"** / **"Down 10% on yesterday"** — shown even on the
  *This week* and *This month* views, where the comparison isn't yesterday.
- Top seller: **"Bakers Inn Bread is flying"** / "13 sold in this stretch".
- Period chips: **"This Week" · "This Month" · "Last Month"** — title case; "Custom".
- Sections: "How they paid", "Selling best".
- Empty: "No sales yet in this stretch." · "Nothing to chart yet."

Rewrite these so each period names its own comparison (today vs yesterday, this week vs last
week, this month vs last month, custom vs the same length before it), the greeting doesn't
address a shop as a person (or drops the name), and the hero label says exactly which period it
covers. The chart bars are default blue today — put them on the token set too.

### What I want back

1. A copy sheet: every string above, final, with plurals and zero cases, keyed by where it
   appears.
2. Mocks: Sync (all states incl. a refused upload expanded), header status (up to date / waiting
   / not sent), sign-in (with the status-bar fix), Stock list and detail (several run-out
   cases), Reports (today, week, month) — phone, plus tablet for Sync and Reports.
3. One line per change saying which rule it follows.
