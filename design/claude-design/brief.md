# ShopMate — whole-app UI redesign brief for Claude Design

Paste everything under **The prompt** into Claude Design. Attach, in this order:

1. Every PNG in `design/claude-design/screens/` (40 screenshots of the app as it is today).
2. `CLAUDE.md` from the repo root — the product's design decisions, which the redesign must keep.
3. `design_handoff_pos_mobile_tablet/ShopMate Phone.dc.html` — the last design pass (POS phone rail
   and tablet tender flow), for reference.
4. **Your logo files** — ShopMate wordmark and symbol (SVG preferred), and any official partner
   marks you have rights to use (EcoCash, InnBucks, OneMoney, Visa, Mastercard). The app has no
   real logo yet: the launcher icon is a generic storefront glyph.

The screenshots use a demo shop (a Harare corner shop, 17 products, four weeks of sales) so every
state is populated. Phone shots are a Pixel 6a (1080×2400); tablet shots a Pixel Tablet
(2560×1600).

---

## The prompt

You are redesigning the complete UI of **ShopMate**, an offline-first Android point-of-sale and
inventory app for small retail shops in Zimbabwe and South Africa. It is built and in use; the
attached screenshots show every screen as it ships today. I want a coherent, production-grade
design system and a redesign of every screen on phone and tablet that my team can rebuild in
Jetpack Compose (Material 3 underneath).

### Who it is for

1. **The cashier** at the till — phone or tablet, all day, often on a thin or absent connection.
   Speed above everything: ringing up a sale must take seconds and work by muscle memory.
2. **The owner or manager** — often not on the premises. Needs to know if something is wrong
   (running out, selling at a loss, money held on a device) before it costs money.

Every screen answers one question for one of them:

| Screen | Question | Measured in |
|---|---|---|
| POS | How fast can I take this money? | seconds |
| Stock | When do I run out? | time |
| Products | Am I still making money on this? | margin |
| Reports | What happened? | period |
| Sync | Where is my data? | queue depth |

### The brand promise — design for it

"No data drops. No Friday surprises." The app tells the truth when the truth is inconvenient:
- Never show a number as certain when it isn't. If something is unknown, say what.
- **Offline is a state, not an error.** Calm and specific ("Held on this device until you're
  back"), never error red, alert icons or apology.
- Stock is a ledger: any stock number must be able to answer "why does it say 6?".
- Surface patterns, not single events.

### Design system — keep these, refine everything else

These decisions are fixed (full detail in the attached CLAUDE.md). Improve execution freely;
change a decision only by flagging it explicitly as a proposal.

- **Two brand colours only.** Trade Green `#005B52` = "attention here" (app frame canvas, primary
  buttons). Checkout Neon `#99FFAA` = "something good happened and it is done" — used only for
  frame identity (brand mark, selected nav pill) and for successful commits (Charge, Commit
  count, Save new price, the success banner edge). Never for selection, hover, focus, "good"
  numbers or trend arrows.
- Structural: Receipt Beige `#D4CEC9` (hairlines, fields), Shop White `#FFFFFF` / Off-White
  `#FAF9F8`, Shop Black `#000000` / `#4A4A4A` / `#8A8A8A` for text.
- **Severity has three tiers and no new colours**: Healthy (no treatment) · Warning
  (`StockOrange` + tinted surface: runs out this week, thin margin, cost rose) · Stopped
  (`ShopBlack` chip, white text: out of stock, selling at a loss). **Error red is for failures
  only**, never for business conditions.
- **Typography: Inter only**, Regular 400 / Medium 500 / SemiBold 600, never Bold. Every number
  uses tabular figures. Eyebrow section labels: 10.5sp Medium, 0.11em tracking, uppercase.
- **App frame:** a full-height Trade Green sidebar on every device (64dp icon-only on phones with
  long-press labels; 84dp icon+label on tablets), Settings pinned at the bottom, content on a
  white sheet with a 24dp rounded top-start corner, a 60dp content header with store name, sync
  status and actions. **No bottom navigation bar, ever.**
- **Two layout buckets only:** COMPACT (phones, stacked) and EXPANDED (tablets/landscape,
  side-by-side). Detail views are a bottom sheet on phone and a right-hand panel on tablet —
  the same content in two containers.
- **Tablet POS** has three zones that never reflow: sidebar · product area · 240dp sale ticket
  with an always-visible number pad. The ticket runs SALE → TENDER → DONE; only its middle band
  changes. Card and mobile-money tenders skip cash/change.
- **Gauges:** one 52×16dp instrument in the same slot on Stock and Products rows —
  `RunwayGauge` (seven day segments, time) and `MarginGauge` (cost track with a target-margin
  notch, money). No third gauge.
- **Copy:** sentence case everywhere; buttons say what happens (never Submit/OK/Confirm); verbs
  carry through (Charge → Charged); name things by what the person controls ("3 sales held on
  this device", not "3 unsynced events"); be specific ("Runs out Thursday", "$6.20 short").

### What exists — read the screenshots in this order

**Phone**
- `phone-00-splash` — launch splash (generic bag glyph on white).
- `phone-01`…`05b` — POS: empty cart, cart with lines, checkout sheet (payment method, cash
  received, change, quick amounts, keypad), cash tendered, charged + success banner.
- `phone-06`, `07` — Sales history list and a receipt.
- `phone-08`…`12` — Products (sections: cost went up, selling at a loss, holding target; unknown
  barcode banner), edit product, new product, categories, unknown codes inbox.
- `phone-13`…`17` — Stock (week-ahead strip, runs out this week, stopped, healthy, not moving),
  product ledger ("why it says 7"), adjust sheet, stock count session.
- `phone-18`, `19`, `23` — Reports: today, scrolled (payments, best sellers), this week.
- `phone-20`, `26` — Sync: waiting, and paused because the session needs sign-in.
- `phone-21`, `22` — Settings.
- `phone-24` — sidebar long-press label.
- `phone-25` — sign-in.
- `phone-27` — POS right after an offline sale ("1 waiting").

**Tablet**
- `tablet-01`…`05` — POS: empty, sale, tender, cash entered, done.
- `tablet-06`…`08` — Products; Stock; Stock with the detail panel.
- `tablet-09`…`11` — Reports, Sync, Settings.

### What is wrong today — fix these

Visual system
1. **No brand identity.** No logo, a generic storefront glyph as brand mark, a generic bag on the
   splash. Design the ShopMate mark (or apply the attached one) across splash, sign-in, sidebar,
   launcher icon and receipts.
2. **Off-brand colour in places.** Reports bars are default chart blue; payment colours are
   arbitrary; the adjust sheet uses red "Sale" chips and Material-default styling; Categories use
   red delete icons. Bring every surface onto the token set.
3. **Inconsistent component language.** The adjust sheet, product form and categories look like
   default Material; Stock and Products look designed. Make one family.
4. Sidebar icons for Stock (bar chart) and Reports (chart) read as the same thing.

POS (speed)
5. Product cards truncate names ("Bakers Inn Br…", "Coca-Col…") while wasting vertical space;
   allow two lines, tighten the card.
6. Phone: the empty cart panel takes a third of the screen.
7. Checkout: quick-cash amounts wrap ($100 alone on a line); "Enter enough cash" as a disabled
   button label; payment methods are plain text — use real marks for EcoCash, card, etc.
8. Tablet tender band: "Cash in / Change out" is cramped and hard to read at arm's length.

Honesty about state
9. The header says **"Up to date" while the device is offline**; offline, waiting, and
   "sign in needed" must be visibly distinct everywhere the sync dot appears — calmly.
10. Settings shows **"Not connected" and "No active store"** when the user is signed in but
    offline.
11. Sync counts a store snapshot as a sale ("2 sales waiting"); the history says "Sale sync",
    "Store snapshot" (system words); the Sync now button spins forever offline.
12. "1 days left" (grammar) on Stock, and "0 days left" in the detail for the same item.
13. Reports compares a week "on yesterday", calls the period "this stretch", and greets the
    store name as if it were a person ("Good afternoon, Jipe K1").
14. Sales history: "3 item(s)", ambiguous cloud icons, no grouping by day, no day totals;
    receipt shows "Cashier: Cashier".

Layout
15. Tablet Reports and Settings use a narrow left column on a wide screen.
16. Tablet Stock: the Count stock button sits over the detail panel.
17. Sign-in: status-bar icons invisible on the light background; screen is generic.

### What I want back

1. **Design system page** — logo usage, colour tokens with roles, type scale (numeric styles
   included), spacing, radii, elevation, iconography, and every component with its states
   (buttons incl. Charge, chips, search, text fields, product card, cart line, number pad,
   sale ticket, InsightRow, LedgerRow, RunwayGauge, MarginGauge, StoppedChip, SectionHeader,
   status badge / sync dot in all states, banners, empty states, sheets and side panels).
2. **Every screen redesigned, phone and tablet**, matching the screenshot list above, including
   the states: empty, loading, offline, sign-in needed, success, out of stock / at a loss.
3. **The two core flows end to end:** ring up and charge a sale (cash and EcoCash), and count
   stock. On tablet, show SALE → TENDER → DONE in the fixed ticket.
4. For each screen, a short note: what changed and why, against the question that screen
   answers.

Constraints: everything must be buildable in Jetpack Compose with Material 3 as the base;
fonts are bundled (Inter only, works offline); touch targets ≥ 48dp; legible in bright shop
light and on cheap low-DPI phones; numbers never shift layout as they change.
