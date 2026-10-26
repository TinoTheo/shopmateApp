'use client';

import { useEffect, useMemo, useState } from 'react';
import { Shell, useStore } from '@/components/Shell';
import { byTill, lines, sales, tills, type Line, type Sale, type Till } from '@/lib/data';
import { dayLabel, money, plural } from '@/lib/format';

export default function ReportsPage() {
  return (
    <Shell title="Reports">
      <Reports />
    </Shell>
  );
}

type Period = 'today' | 'week' | 'month' | 'lastMonth';
const PERIODS: { key: Period; label: string }[] = [
  { key: 'today', label: 'Today' },
  { key: 'week', label: 'This week' },
  { key: 'month', label: 'This month' },
  { key: 'lastMonth', label: 'Last month' },
];

function range(p: Period): { from: Date; to: Date; open: boolean } {
  const now = new Date();
  const day = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  switch (p) {
    case 'today':
      return { from: day, to: new Date(day.getTime() + 86400000), open: true };
    case 'week': {
      const monday = new Date(day);
      monday.setDate(day.getDate() - ((day.getDay() + 6) % 7));
      return { from: monday, to: new Date(monday.getTime() + 7 * 86400000), open: true };
    }
    case 'month':
      return { from: new Date(now.getFullYear(), now.getMonth(), 1), to: new Date(now.getFullYear(), now.getMonth() + 1, 1), open: true };
    case 'lastMonth':
      return { from: new Date(now.getFullYear(), now.getMonth() - 1, 1), to: new Date(now.getFullYear(), now.getMonth(), 1), open: false };
  }
}

const METHOD: Record<string, string> = {
  CASH: 'Cash', ECOCASH: 'EcoCash', MOBILE_MONEY: 'Mobile money', CARD: 'Card', BANK_TRANSFER: 'Bank transfer',
};

/** The store's sales across every till — what the till app's Reports can't show on its own. */
function Reports() {
  const store = useStore();
  const [period, setPeriod] = useState<Period>(() => {
    if (typeof window === 'undefined') return 'today';
    const q = new URLSearchParams(window.location.search).get('period');
    return (PERIODS.find((p) => p.key === q)?.key ?? 'today') as Period;
  });
  const [rows, setRows] = useState<Sale[] | null>(null);
  const [items, setItems] = useState<Line[]>([]);
  const [tillList, setTills] = useState<Till[]>([]);
  const [error, setError] = useState<string | null>(null);
  const { from, to, open } = useMemo(() => range(period), [period]);

  useEffect(() => {
    setRows(null);
    setError(null);
    Promise.all([sales(store.id, from, to), tills(store.id)])
      .then(async ([s, t]) => {
        setRows(s);
        setTills(t);
        setItems(await lines(s.map((x) => x.id)));
      })
      .catch((e: Error) => setError(e.message));
  }, [store.id, from, to]);

  const total = rows?.reduce((a, s) => a + s.total_amount, 0) ?? 0;

  // Bars: by hour today, by day otherwise. The bar still filling is lighter (as on the till).
  const bars = useMemo(() => {
    if (!rows) return [];
    if (period === 'today') {
      const nowHour = new Date().getHours();
      return Array.from({ length: 24 }, (_, h) => ({
        label: `${h}`,
        value: rows.filter((s) => new Date(s.created_at).getHours() === h).reduce((a, s) => a + s.total_amount, 0),
        partial: h === nowHour,
        future: h > nowHour,
      }));
    }
    const out = [];
    const today = new Date().toDateString();
    for (let d = new Date(from); d < to; d = new Date(d.getTime() + 86400000)) {
      const key = d.toDateString();
      out.push({
        label: period === 'week' ? d.toLocaleDateString('en-GB', { weekday: 'short' }) : `${d.getDate()}`,
        value: rows.filter((s) => new Date(s.created_at).toDateString() === key).reduce((a, s) => a + s.total_amount, 0),
        partial: open && key === today,
        future: d.getTime() > Date.now(),
      });
    }
    return out;
  }, [rows, period, from, to, open]);
  const max = Math.max(1, ...bars.map((b) => b.value));

  const best = useMemo(() => {
    const m = new Map<string, { qty: number; total: number }>();
    for (const l of items) {
      const e = m.get(l.product_name) ?? { qty: 0, total: 0 };
      e.qty += l.quantity;
      e.total += l.line_total;
      m.set(l.product_name, e);
    }
    return [...m.entries()].sort((a, b) => b[1].total - a[1].total).slice(0, 10);
  }, [items]);

  const methods = useMemo(() => {
    const m = new Map<string, number>();
    for (const s of rows ?? []) m.set(s.payment_method, (m.get(s.payment_method) ?? 0) + s.total_amount);
    return [...m.entries()].sort((a, b) => b[1] - a[1]);
  }, [rows]);

  const perTill = rows ? byTill(rows) : new Map();
  const tillName = (code: string) => tillList.find((t) => t.code === code)?.name ?? (code === '—' ? 'Before till codes' : `Till ${code}`);

  return (
    <>
      <div className="chips" role="tablist">
        {PERIODS.map((p) => (
          <button key={p.key} className={p.key === period ? 'on' : ''} onClick={() => setPeriod(p.key)}>{p.label}</button>
        ))}
      </div>

      {error && <p className="note">Couldn&apos;t load this report: {error}</p>}
      {!rows && !error && <p className="note">Loading…</p>}
      {rows && (
        <>
          <div className="grid" style={{ marginTop: 16 }}>
            <div className="card">
              <div className="label">{period === 'today' ? 'Sales today' : `Sales, ${dayLabel(from)} to ${dayLabel(new Date(to.getTime() - 1))}`}{open ? ', so far' : ''}</div>
              <div className="value num">{money(total, store.currency_symbol)}</div>
              <div className="sub num">{plural(rows.length, 'sale', 'sales')}{rows.length ? ` · ${money(total / rows.length, store.currency_symbol)} average sale` : ''}</div>
            </div>
          </div>

          <div className="eyebrow">{period === 'today' ? 'By hour' : 'By day'}</div>
          {rows.length === 0 ? (
            <p className="note">{open ? 'No sales yet in this period.' : 'No sales in this period.'}</p>
          ) : (
            <>
              <div className="bars">
                {bars.map((b, i) => (
                  <div key={i} className={`bar${b.partial ? ' partial' : ''}`} title={money(b.value, store.currency_symbol)}
                    style={{ height: `${(b.value / max) * 100}%`, visibility: b.future ? 'hidden' : 'visible' }} />
                ))}
              </div>
              <div className="bar-labels num">{bars.map((b, i) => <span key={i}>{b.label}</span>)}</div>
              {open && <p className="muted" style={{ fontSize: 11.5 }}>Lighter bar: {period === 'today' ? 'this hour' : 'today'}, still going</p>}
            </>
          )}

          <div className="grid" style={{ marginTop: 8 }}>
            <section>
              <div className="eyebrow">By till</div>
              <table>
                <tbody>
                  {[...perTill.entries()].sort((a, b) => b[1].total - a[1].total).map(([code, t]) => (
                    <tr key={code}><td>{tillName(code)}</td><td className="r num">{t.count}</td><td className="r num">{money(t.total, store.currency_symbol)}</td></tr>
                  ))}
                </tbody>
              </table>
            </section>
            <section>
              <div className="eyebrow">Payments</div>
              <table>
                <tbody>
                  {methods.map(([m, v]) => (
                    <tr key={m}><td>{METHOD[m] ?? m}</td><td className="r num">{money(v, store.currency_symbol)}</td><td className="r num muted">{total ? Math.round((v / total) * 100) : 0}%</td></tr>
                  ))}
                </tbody>
              </table>
            </section>
          </div>

          <div className="eyebrow">Best sellers</div>
          <table>
            <thead><tr><th>Product</th><th className="r">Sold</th><th className="r">Taken</th></tr></thead>
            <tbody>
              {best.map(([name, b]) => (
                <tr key={name}><td>{name}</td><td className="r num">{b.qty}</td><td className="r num">{money(b.total, store.currency_symbol)}</td></tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </>
  );
}
