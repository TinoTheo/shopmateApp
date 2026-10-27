import type { Movement, Product, Sale, Till } from './data';
import {
  addDays, daysBetween, duration, hm, listOf, longDate, monthName, ordinal, sameDay, shortDate,
  startOfDay, tillCodeOf, weekday, weekdayShort,
} from './format';

// Everything the screens show is derived here, on read, from sales, tills and the ledger. Nothing
// is stored (CLAUDE.md § Derived, never stored), so the list and the detail can't disagree.

/* ───────────────────────── Tills ───────────────────────── */

/** No contact for longer than this, while the shop is trading, and a till is quiet. */
export const QUIET_MS = 60 * 60 * 1000;

export type TillView = {
  till: Till;
  lastSeen: Date;
  removed: boolean;
  /** Quiet: trading today, and not heard from for over an hour. Its sales after [lastSeen] may be missing. */
  quiet: boolean;
  quietFor: string;
  salesToday: number;
  takenToday: number;
};

/**
 * The store is trading today once any till has sent a sale today. Before that, a till that's
 * been off all night isn't "quiet" — nobody is missing anything yet. (There are no trading hours
 * on the server; this is the nearest honest stand-in.)
 */
export function tillViews(tills: Till[], today: Sale[], now: Date): TillView[] {
  const trading = today.length > 0;
  const byCode = new Map<string, { n: number; total: number }>();
  for (const s of today) {
    const code = tillCodeOf(s.receipt_number);
    if (!code) continue;
    const e = byCode.get(code) ?? { n: 0, total: 0 };
    e.n += 1;
    e.total += s.total_amount;
    byCode.set(code, e);
  }
  return tills.map((till) => {
    const lastSeen = new Date(till.last_seen_at);
    const removed = till.revoked_at != null;
    const gap = now.getTime() - lastSeen.getTime();
    const mine = byCode.get(till.code);
    return {
      till,
      lastSeen,
      removed,
      quiet: !removed && trading && gap > QUIET_MS,
      quietFor: duration(gap),
      salesToday: mine?.n ?? 0,
      takenToday: mine?.total ?? 0,
    };
  });
}

/** Names for till codes on receipts; a code no till claims reads "Till K7Q". */
export function tillNamer(tills: Till[]): (code: string | null) => string {
  const names = new Map(tills.map((t) => [t.code, t.name]));
  return (code) => (code ? names.get(code) ?? `Till ${code}` : 'Older receipts');
}

/** "Till 2 after 13:58", or "Till 2 today" when it hasn't been heard from at all today. */
function missingPart(t: TillView, now: Date): string {
  return sameDay(t.lastSeen, now) ? `${t.till.name} after ${hm(t.lastSeen)}` : `${t.till.name} today`;
}

/** The uncertainty line under a total that includes today, or null when every till has reported. */
export function missingLine(quiet: TillView[], now: Date, kind: 'today' | 'period'): string | null {
  if (!quiet.length) return null;
  const parts = listOf(quiet.map((t) => missingPart(t, now))).replace(/ and /g, ' or ');
  if (kind === 'period') return `Doesn’t include ${parts}${parts.endsWith('today') ? '' : ' today'}.`;
  const who = quiet.length === 1 ? 'it reports' : 'they report';
  return `Doesn’t include ${parts}. The total and the comparison may rise when ${who}.`;
}

/* ───────────────────────── Periods ───────────────────────── */

export type PeriodKey = 'today' | 'week' | 'month' | 'lastMonth' | 'range';

export type Period = {
  key: PeriodKey;
  from: Date;
  /** Exclusive. For an open period, now. */
  to: Date;
  prevFrom: Date;
  prevTo: Date;
  open: boolean;
  /** "Monday 19 to Thursday 22 October, so far". */
  subtitle: string;
  /** "Sales this week, so far". */
  eyebrow: string;
  /** What the comparison is against: "yesterday", "last week", "September", "the 7 days before". */
  against: string;
  /** "by 15:29", "by Thursday 15:29", "by the 22nd", or "" for a closed period. */
  by: string;
  granularity: 'hour' | 'day';
};

function mondayOf(d: Date): Date {
  const s = startOfDay(d);
  return addDays(s, -((s.getDay() + 6) % 7));
}

/** "Monday 19 to Thursday 22 October", "Monday 28 September to Thursday 1 October". */
function span(a: Date, b: Date, withWeekdays: boolean): string {
  const left = withWeekdays ? `${weekday(a)} ${a.getDate()}` : `${a.getDate()}`;
  const right = withWeekdays ? longDate(b) : `${b.getDate()} ${monthName(b)}`;
  if (sameDay(a, b)) return right;
  return a.getMonth() === b.getMonth() ? `${left} to ${right}` : `${left} ${monthName(a)} to ${right}`;
}

export function period(key: PeriodKey, now: Date, range?: { from: Date; to: Date }): Period {
  const today = startOfDay(now);
  if (key === 'today') {
    return {
      key, from: today, to: now, prevFrom: addDays(today, -1), prevTo: addDays(now, -1), open: true,
      subtitle: `${longDate(now)}, so far`, eyebrow: 'Sales today, so far',
      against: 'yesterday', by: `by ${hm(now)}`, granularity: 'hour',
    };
  }
  if (key === 'week') {
    const from = mondayOf(now);
    return {
      key, from, to: now, prevFrom: addDays(from, -7), prevTo: addDays(now, -7), open: true,
      subtitle: `${span(from, now, true)}, so far`, eyebrow: 'Sales this week, so far',
      against: 'last week', by: `by ${weekday(now)} ${hm(now)}`, granularity: 'day',
    };
  }
  if (key === 'month') {
    const from = new Date(now.getFullYear(), now.getMonth(), 1);
    const prevFrom = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const prevDays = new Date(now.getFullYear(), now.getMonth(), 0).getDate();
    const prevTo = new Date(prevFrom);
    prevTo.setDate(Math.min(now.getDate(), prevDays));
    prevTo.setHours(now.getHours(), now.getMinutes(), now.getSeconds());
    return {
      key, from, to: now, prevFrom, prevTo, open: true,
      subtitle: `${span(from, now, false)}, so far`, eyebrow: 'Sales this month, so far',
      against: monthName(prevFrom), by: `by the ${ordinal(now.getDate())}`, granularity: 'day',
    };
  }
  if (key === 'lastMonth') {
    const from = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const to = new Date(now.getFullYear(), now.getMonth(), 1);
    const prevFrom = new Date(now.getFullYear(), now.getMonth() - 2, 1);
    return {
      key, from, to, prevFrom, prevTo: from, open: false,
      subtitle: span(from, addDays(to, -1), false), eyebrow: `Sales in ${monthName(from)}`,
      against: monthName(prevFrom), by: '', granularity: 'day',
    };
  }
  const a = startOfDay(range?.from ?? addDays(today, -6));
  const b = startOfDay(range?.to ?? today);
  const to = addDays(b, 1);
  const open = to > now;
  const days = daysBetween(a, to);
  return {
    key: 'range', from: a, to: open ? now : to, prevFrom: addDays(a, -days), prevTo: open ? addDays(now, -days) : a, open,
    subtitle: `${span(a, b, false)}${open ? ', so far' : ''}`, eyebrow: `Sales, ${span(a, b, false)}${open ? ', so far' : ''}`,
    against: days === 1 ? 'the day before' : `the ${days} days before`, by: '', granularity: days === 1 ? 'hour' : 'day',
  };
}

export type Comparison = { dir: 'up' | 'down' | 'level' | 'none'; text: string };

/** Like with like: the same span of the previous period, up to the same time (§ Copy — Reports). */
export function compare(current: number, previous: number, p: Period): Comparison {
  const by = p.by ? ` ${p.by}` : '';
  if (previous <= 0) return { dir: 'none', text: 'Nothing to compare with yet' };
  const change = (current - previous) / previous;
  const pct = Math.round(Math.abs(change) * 100);
  if (pct === 0) return { dir: 'level', text: `Level with ${p.against}${by}` };
  return change > 0
    ? { dir: 'up', text: `Up ${pct}% on ${p.against}${by}` }
    : { dir: 'down', text: `Down ${pct}% on ${p.against}${by}` };
}

/* ───────────────────────── Charts ───────────────────────── */

export type Bucket = { label: string; value: number; state: 'done' | 'open' | 'future' };

/** By hour, 07–18 at least, widened to any hour with sales; the current hour is open. */
export function byHour(rows: Sale[], day: Date, now: Date): Bucket[] {
  const totals = new Map<number, number>();
  for (const s of rows) {
    const h = new Date(s.created_at).getHours();
    totals.set(h, (totals.get(h) ?? 0) + s.total_amount);
  }
  const isToday = sameDay(day, now);
  const hours = [...totals.keys(), 7, 18, ...(isToday ? [now.getHours()] : [])];
  const out: Bucket[] = [];
  for (let h = Math.min(...hours); h <= Math.max(...hours); h++) {
    const state = !isToday || h < now.getHours() ? 'done' : h === now.getHours() ? 'open' : 'future';
    out.push({ label: String(h).padStart(2, '0'), value: totals.get(h) ?? 0, state });
  }
  return out;
}

/** By day from [from] to [end) — Mon–Sun for a week; today is open, later days are stubs. */
export function byDay(rows: Sale[], from: Date, end: Date, now: Date, labels: 'weekday' | 'date'): Bucket[] {
  const totals = new Map<string, number>();
  for (const s of rows) {
    const k = new Date(s.created_at).toDateString();
    totals.set(k, (totals.get(k) ?? 0) + s.total_amount);
  }
  const out: Bucket[] = [];
  for (let d = startOfDay(from); d < end; d = addDays(d, 1)) {
    const state = sameDay(d, now) ? 'open' : d > now ? 'future' : 'done';
    out.push({ label: labels === 'weekday' ? weekdayShort(d) : String(d.getDate()), value: totals.get(d.toDateString()) ?? 0, state });
  }
  return out;
}

/** Round axis steps: $0/$50/$100/$150 rather than $0/$47/$94. */
export function axis(max: number, ticks: number): number[] {
  if (max <= 0) return [0, 1];
  const raw = max / ticks;
  const mag = 10 ** Math.floor(Math.log10(raw));
  const step = [1, 2, 2.5, 5, 10].map((m) => m * mag).find((s) => s >= raw) ?? raw;
  const top = Math.ceil(max / step) * step;
  const out: number[] = [];
  for (let v = 0; v <= top + step / 2; v += step) out.push(Math.round(v * 100) / 100);
  return out;
}

/* ───────────────────────── Stock ───────────────────────── */

export const VELOCITY_WINDOW_DAYS = 28;
export const MIN_CONFIDENT_DAYS = 7;
export const DORMANT_DAYS = 14;

/** CLAUDE.md § States the copy pass fixed — one RunOut, read by the list and the detail alike. */
export type RunOut =
  | { kind: 'out'; since: Date | null }
  | { kind: 'below'; by: number }
  | { kind: 'today' }
  | { kind: 'tomorrow' }
  | { kind: 'day'; day: string; ahead: number }
  | { kind: 'week' }
  | { kind: 'weeks'; weeks: number }
  | { kind: 'month' }
  | { kind: 'noHistory' }
  | { kind: 'dormant'; days: number };

export type StockItem = {
  product: Product;
  quantity: number;
  unitsPerDay: number | null;
  runOut: RunOut;
  section: 'out' | 'soon' | 'healthy';
  /** Days of cover, for sorting; Infinity when it can't be projected. */
  cover: number;
  /** Sold today — a quiet till could have sold more of it. */
  soldToday: boolean;
  movements: Movement[];
};

function runOutOf(quantity: number, unitsPerDay: number | null, sinceLastSale: number | null, outSince: Date | null, now: Date): RunOut {
  if (quantity < 0) return { kind: 'below', by: -quantity };
  if (quantity === 0) return { kind: 'out', since: outSince };
  if (sinceLastSale != null && sinceLastSale >= DORMANT_DAYS) return { kind: 'dormant', days: sinceLastSale };
  if (unitsPerDay == null || unitsPerDay <= 0) return { kind: 'noHistory' };
  // The day comes from comparing calendar dates, never from rounding a count of days.
  const date = new Date(now.getTime() + (quantity / unitsPerDay) * 86_400_000);
  const days = daysBetween(now, date);
  if (days <= 0) return { kind: 'today' };
  if (days === 1) return { kind: 'tomorrow' };
  if (days <= 6) return { kind: 'day', day: weekday(date), ahead: days };
  if (days <= 13) return { kind: 'week' };
  if (days <= 27) return { kind: 'weeks', weeks: Math.floor(days / 7) };
  return { kind: 'month' };
}

export function stockItems(products: Product[], ledger: Movement[], now: Date): StockItem[] {
  const byProduct = new Map<string, Movement[]>();
  for (const m of ledger) {
    const list = byProduct.get(m.product_id);
    if (list) list.push(m);
    else byProduct.set(m.product_id, [m]);
  }
  const windowStart = addDays(startOfDay(now), -VELOCITY_WINDOW_DAYS);
  return products.map((product) => {
    const moves = byProduct.get(product.id) ?? [];
    let quantity = 0;
    let outSince: Date | null = null;
    let units = 0;
    let firstSale: Date | null = null;
    let lastSale: Date | null = null;
    const days = new Set<string>();
    for (const m of moves) {
      const before = quantity;
      quantity += m.delta;
      const at = new Date(m.created_at);
      if (before > 0 && quantity <= 0) outSince = at;
      if (quantity > 0) outSince = null;
      if (m.reason === 'SALE') {
        lastSale = at;
        if (at >= windowStart) {
          units += -m.delta;
          days.add(at.toDateString());
          firstSale ??= at;
        }
      }
    }
    // Velocity confidence rule: under 7 days of selling in the window, and we don't project.
    const confident = days.size >= MIN_CONFIDENT_DAYS && units > 0;
    const span = firstSale ? Math.min(Math.max(Math.floor((now.getTime() - firstSale.getTime()) / 86_400_000), 1), VELOCITY_WINDOW_DAYS) : 1;
    const unitsPerDay = confident ? units / span : null;
    const sinceLastSale = lastSale ? Math.floor((now.getTime() - lastSale.getTime()) / 86_400_000) : null;
    const runOut = runOutOf(quantity, unitsPerDay, sinceLastSale, outSince, now);
    const section = quantity <= 0 ? 'out' : ['today', 'tomorrow', 'day'].includes(runOut.kind) ? 'soon' : 'healthy';
    return {
      product,
      quantity,
      unitsPerDay,
      runOut,
      section,
      cover: unitsPerDay && quantity > 0 ? quantity / unitsPerDay : Infinity,
      soldToday: lastSale != null && sameDay(lastSale, now),
      movements: moves,
    };
  });
}

/** "Out since 11:40", "Out since Mon", "Out since 12 Oct". */
export function outSinceText(since: Date | null, now: Date): string {
  if (!since) return 'Out of stock';
  if (sameDay(since, now)) return `Out since ${hm(since)}`;
  return `Out since ${daysBetween(since, now) < 7 ? weekdayShort(since) : shortDate(since)}`;
}

/** The chip or plain words for a row — only problems get a chip. */
export function runOutLabel(r: RunOut): string {
  switch (r.kind) {
    case 'out': return 'Out';
    case 'below': return 'Below zero';
    case 'today': return 'Runs out today';
    case 'tomorrow': return 'Runs out tomorrow';
    case 'day': return `Runs out ${r.day}`;
    case 'week': return 'Over a week';
    case 'weeks': return `Over ${r.weeks} weeks`;
    case 'month': return 'Over a month';
    case 'noHistory': return 'Not enough sales to tell yet';
    case 'dormant': return `Not sold in ${r.days} days`;
  }
}

export function belowZeroText(by: number): string {
  return `Sold ${by} more than recorded. Count it on a till to correct it.`;
}

/** "about 15", "about 2.5", "under 1" a day. */
export function perDay(n: number): string {
  if (n < 1) return n >= 0.5 ? `about ${n.toFixed(1)}` : 'under 1';
  return `about ${n < 10 ? Math.round(n * 10) / 10 : Math.round(n)}`;
}

export function sortItems(items: StockItem[]): StockItem[] {
  return [...items].sort((a, b) => {
    if (a.section === 'out' && b.section === 'out') {
      return (a.quantity === 0 ? 0 : 1) - (b.quantity === 0 ? 0 : 1) || a.product.name.localeCompare(b.product.name);
    }
    return a.cover - b.cover || a.product.name.localeCompare(b.product.name);
  });
}

/* ───────────────────────── Ledger ───────────────────────── */

export type LedgerRow = { kind: string; meta: string; change: string; total: number };

function kindOf(m: Movement): string {
  switch (m.reason) {
    case 'SALE': return 'Sold';
    case 'COUNT': return 'Count';
    case 'PURCHASE':
    case 'RESTOCK': return m.note === 'Opening stock' ? 'Opening stock' : 'Delivery';
    case 'CORRECTION': return m.note ? `Correction: ${m.note.toLowerCase()}` : 'Correction';
    case 'MANUAL_ADJUSTMENT': return m.note ? `Adjustment: ${m.note.toLowerCase()}` : 'Adjustment';
    default: return 'Change';
  }
}

/** "Today 15:27", "Mon 07:50", "12 Oct 07:50". */
function when(d: Date, now: Date): string {
  if (sameDay(d, now)) return `Today ${hm(d)}`;
  if (daysBetween(d, now) < 7) return `${weekdayShort(d)} ${hm(d)}`;
  return `${shortDate(d)} ${hm(d)}`;
}

function dayWord(d: Date, now: Date): string {
  if (sameDay(d, now)) return 'Today';
  return daysBetween(d, now) < 7 ? weekdayShort(d) : shortDate(d);
}

/**
 * "Why does it say 6?" — movements since the last count, oldest first, sales grouped per day
 * with the tills that made them. Opens with the count, so the running total starts from a
 * number someone saw on the shelf.
 */
export function ledgerRows(item: StockItem, tills: Till[], now: Date): LedgerRow[] {
  const moves = item.movements;
  let start = 0;
  for (let i = moves.length - 1; i >= 0; i--) {
    if (moves[i].reason === 'COUNT') { start = i; break; }
  }
  let total = moves.slice(0, start).reduce((n, m) => n + m.delta, 0);
  const name = tillNamer(tills);
  const rows: LedgerRow[] = [];
  let i = start;
  while (i < moves.length) {
    const m = moves[i];
    const at = new Date(m.created_at);
    if (m.reason === 'SALE') {
      // Every sale that day, in one row.
      let sum = 0;
      let last = at;
      const codes = new Set<string>();
      while (i < moves.length && moves[i].reason === 'SALE' && sameDay(new Date(moves[i].created_at), at)) {
        sum += moves[i].delta;
        last = new Date(moves[i].created_at);
        const code = tillCodeOf(moves[i].note);
        if (code) codes.add(code);
        i++;
      }
      total += sum;
      const tillNames = [...codes].map((c) => name(c)).sort();
      const who = tillNames.length ? `${tillNames.length > 1 ? 'Tills ' + listOf(tillNames.map((n) => n.replace(/^Till /, ''))) : tillNames[0]} · ` : '';
      const day = sameDay(at, now) ? `Today to ${hm(last)}` : dayWord(at, now);
      rows.push({ kind: 'Sold', meta: `${who}${day}`, change: sum < 0 ? `−${-sum}` : `+${sum}`, total });
      continue;
    }
    total += m.delta;
    const count = m.reason === 'COUNT';
    rows.push({
      kind: kindOf(m),
      meta: m.actor ? `${m.actor} · ${when(at, now)}` : when(at, now),
      change: count ? String(total) : m.delta > 0 ? `+${m.delta}` : `−${-m.delta}`,
      total,
    });
    i++;
  }
  return rows;
}

/* ───────────────────────── Needs you ───────────────────────── */

export type Attention = {
  key: string;
  tone: 'warning' | 'stopped';
  chip: string;
  title: string;
  body: string;
  action: string;
  href: string;
};

/**
 * Ranked: anything that makes the numbers on screen uncertain (quiet tills), then stopped (out,
 * below zero), then warning (short counts today, runs out today). Empty means nothing needs you.
 */
export function attention(views: TillView[], items: StockItem[], symbol: string, now: Date, offline = false): Attention[] {
  const out: Attention[] = [];
  const hasnt = offline ? 'hadn’t' : 'hasn’t';
  for (const t of views.filter((v) => v.quiet)) {
    const since = sameDay(t.lastSeen, now) ? hm(t.lastSeen) : whenShortDay(t.lastSeen, now);
    out.push({
      key: `till-${t.till.id}`, tone: 'warning', chip: 'Quiet',
      title: `${t.till.name} ${hasnt} reported since ${since}`,
      body: `Its sales after ${since} aren’t in today’s total. It may be offline or switched off.`,
      action: `See ${t.till.name}`, href: `/tills/#till-${t.till.id}`,
    });
  }
  for (const it of items.filter((i) => i.section === 'out')) {
    const below = it.runOut.kind === 'below';
    out.push({
      key: `out-${it.product.id}`, tone: 'stopped', chip: below ? 'Below zero' : 'Out',
      title: it.product.name,
      body: below ? belowZeroText(-it.quantity) : `${outSinceText(it.runOut.kind === 'out' ? it.runOut.since : null, now).replace('Out since', 'Out of stock since')}.`,
      action: 'See stock', href: `/stock/?product=${it.product.id}`,
    });
  }
  const today = startOfDay(now);
  for (const it of items) {
    let running = 0;
    for (const m of it.movements) {
      running += m.delta;
      const at = new Date(m.created_at);
      if (m.reason !== 'COUNT' || m.delta >= 0 || at < today) continue;
      const short = -m.delta;
      const who = m.actor ? `${m.actor} counted` : 'Counted';
      out.push({
        key: `count-${m.id}`, tone: 'warning', chip: `${short} short`,
        title: `${it.product.name} count`,
        body: `${who} ${running} at ${hm(at)}; records said ${running + short}. ${moneyAt(short * it.product.selling_price, symbol)} at selling price.`,
        action: 'See history', href: `/stock/?product=${it.product.id}`,
      });
    }
  }
  const todayRuns = items.filter((i) => i.runOut.kind === 'today').sort((a, b) => a.cover - b.cover);
  const laterThisWeek = items.filter((i) => i.runOut.kind === 'tomorrow' || i.runOut.kind === 'day').length;
  todayRuns.forEach((it, idx) => {
    const more = idx === todayRuns.length - 1 && laterThisWeek
      ? ` ${laterThisWeek} more run${laterThisWeek === 1 ? 's' : ''} out this week.` : '';
    out.push({
      key: `runs-${it.product.id}`, tone: 'warning', chip: 'Runs out today',
      title: it.product.name,
      body: `${it.quantity} left, selling ${perDay(it.unitsPerDay ?? 0)} a day.${more}`,
      action: 'See stock', href: `/stock/?product=${it.product.id}`,
    });
  });
  return out;
}

function whenShortDay(d: Date, now: Date): string {
  return daysBetween(d, now) < 7 ? `${weekdayShort(d)} ${hm(d)}` : `${shortDate(d)} ${hm(d)}`;
}

function moneyAt(n: number, symbol: string): string {
  return `${symbol}${n.toLocaleString('en', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/* ───────────────────────── Payments ───────────────────────── */

export const PAYMENTS: { key: string; label: string; className: string }[] = [
  { key: 'CASH', label: 'Cash', className: 'pay-cash' },
  { key: 'ECOCASH', label: 'EcoCash', className: 'pay-ecocash' },
  { key: 'MOBILE_MONEY', label: 'Mobile money', className: 'pay-mobile' },
  { key: 'CARD', label: 'Card', className: 'pay-card' },
  { key: 'BANK_TRANSFER', label: 'Bank transfer', className: 'pay-bank' },
];
