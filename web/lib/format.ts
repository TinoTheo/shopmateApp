/** Money in the store's symbol, two places — the till app's CurrencyFormatter, on the web. */
export function money(amount: number, symbol = '$'): string {
  const sign = amount < 0 ? '-' : '';
  return `${sign}${symbol}${Math.abs(amount).toLocaleString('en', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/** "10:42" today, "25 Oct, 10:42" before. */
export function whenSaid(iso: string): string {
  const d = new Date(iso);
  const today = new Date();
  const time = d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  if (d.toDateString() === today.toDateString()) return time;
  return `${d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })}, ${time}`;
}

export function dayLabel(d: Date): string {
  return d.toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' });
}

/** Receipt numbers carry the till code: SM-YYYYMMDD-TTT-XXXX. Older ones have none. */
export function tillCodeOf(receipt: string): string | null {
  const parts = receipt.split('-');
  return parts.length === 4 ? parts[2] : null;
}

export function plural(n: number, one: string, other: string): string {
  return `${n.toLocaleString('en')} ${n === 1 ? one : other}`;
}
