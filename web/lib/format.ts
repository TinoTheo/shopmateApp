/** Money in the store's symbol, two places — the till app's CurrencyFormatter, on the web. */
export function money(amount: number, symbol = '$'): string {
  const sign = amount < 0 ? '−' : '';
  return `${sign}${symbol}${Math.abs(amount).toLocaleString('en', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/** Axis money: whole units, no cents ("$150"). */
export function moneyShort(amount: number, symbol = '$'): string {
  return `${symbol}${Math.round(amount).toLocaleString('en')}`;
}

/** A signed whole number with a real minus sign: "+44", "−17", "0". */
export function signed(n: number): string {
  if (n > 0) return `+${n}`;
  if (n < 0) return `−${Math.abs(n)}`;
  return '0';
}

/** A quantity with a real minus sign: "6", "−3". */
export function qty(n: number): string {
  return n < 0 ? `−${Math.abs(n)}` : String(n);
}

export function hm(d: Date): string {
  return d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
}

export function weekday(d: Date): string {
  return d.toLocaleDateString('en-GB', { weekday: 'long' });
}

export function weekdayShort(d: Date): string {
  return d.toLocaleDateString('en-GB', { weekday: 'short' });
}

export function monthName(d: Date): string {
  return d.toLocaleDateString('en-GB', { month: 'long' });
}

/** "Thursday 22 October". */
export function longDate(d: Date): string {
  return `${weekday(d)} ${d.getDate()} ${monthName(d)}`;
}

/** "22 Oct". */
export function shortDate(d: Date): string {
  return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
}

export function ordinal(n: number): string {
  const s = n % 100 >= 11 && n % 100 <= 13 ? 'th' : ({ 1: 'st', 2: 'nd', 3: 'rd' } as Record<number, string>)[n % 10] ?? 'th';
  return `${n}${s}`;
}

export function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

export function addDays(d: Date, n: number): Date {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
}

export function sameDay(a: Date, b: Date): boolean {
  return a.toDateString() === b.toDateString();
}

/** Calendar days from [a] to [b], by date, never by rounding hours. */
export function daysBetween(a: Date, b: Date): number {
  return Math.round((startOfDay(b).getTime() - startOfDay(a).getTime()) / 86_400_000);
}

/** "15:27" today, "Mon 18:02" this week, "12 Oct 18:02" before. */
export function whenShort(d: Date, now = new Date()): string {
  if (sameDay(d, now)) return hm(d);
  if (daysBetween(d, now) < 7) return `${weekdayShort(d)} ${hm(d)}`;
  return `${shortDate(d)} ${hm(d)}`;
}

/** "today 15:27", "Mon 18:02", "12 Oct 18:02" — for "last heard from". */
export function heardAt(d: Date, now = new Date()): string {
  return sameDay(d, now) ? `today ${hm(d)}` : whenShort(d, now);
}

/** "1 h 31 min", "45 min", "3 h", "2 days". */
export function duration(ms: number): string {
  const min = Math.floor(ms / 60_000);
  if (min < 60) return `${min} min`;
  const h = Math.floor(min / 60);
  if (h >= 48) return `${Math.floor(h / 24)} days`;
  const m = min % 60;
  return m ? `${h} h ${m} min` : `${h} h`;
}

/** Receipt numbers carry the till code: SM-YYYYMMDD-TTT-XXXX. Older ones have none. */
export function tillCodeOf(receipt: string | null | undefined): string | null {
  if (!receipt) return null;
  const parts = receipt.split('-');
  return parts.length === 4 ? parts[2] : null;
}

export function plural(n: number, one: string, other: string): string {
  return `${n.toLocaleString('en')} ${n === 1 ? one : other}`;
}

/** "Till 1", "Till 1 and Till 2", "Till 1, Till 2 and Till 3". */
export function listOf(items: string[]): string {
  if (items.length <= 1) return items[0] ?? '';
  return `${items.slice(0, -1).join(', ')} and ${items[items.length - 1]}`;
}
