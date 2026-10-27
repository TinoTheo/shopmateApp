'use client';

import { useEffect, useState } from 'react';
import { Shell, useStore } from '@/components/Shell';
import { BarChart, Compare, Failed, Head, Offline, Skeleton, Stats, Unsure, Updated } from '@/components/ui';
import { lines, sales, tills, type Sale } from '@/lib/data';
import { addDays, heardAt, hm, money, plural, sameDay, startOfDay, tillCodeOf } from '@/lib/format';
import { byDay, byHour, compare, missingLine, PAYMENTS, period, tillNamer, tillViews, type PeriodKey } from '@/lib/model';
import { useLoad } from '@/lib/useLoad';

export default function ReportsPage() {
  return (
    <Shell>
      <Reports />
    </Shell>
  );
}

const CHIPS: { key: PeriodKey; label: string }[] = [
  { key: 'today', label: 'Today' },
  { key: 'week', label: 'This week' },
  { key: 'month', label: 'This month' },
  { key: 'lastMonth', label: 'Last month' },
  { key: 'range', label: 'Pick dates' },
];

const FAILED: Record<PeriodKey, string> = {
  today: 'Today’s report didn’t load',
  week: 'This week’s report didn’t load',
  month: 'This month’s report didn’t load',
  lastMonth: 'Last month’s report didn’t load',
  range: 'This report didn’t load',
};

const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const fromIso = (s: string) => {
  const [y, m, d] = s.split('-').map(Number);
  return new Date(y, m - 1, d);
};

/** Reports (handoff 1f–1h): what happened, compared like with like, across every till. */
function Reports() {
  const { store, symbol } = useStore();
  const [key, setKey] = useState<PeriodKey>('week');
  const [range, setRange] = useState(() => ({ from: iso(addDays(new Date(), -6)), to: iso(new Date()) }));

  useEffect(() => {
    const q = new URLSearchParams(window.location.search).get('period');
    const known: Record<string, PeriodKey> = { today: 'today', week: 'week', month: 'month', 'last-month': 'lastMonth', lastMonth: 'lastMonth' };
    if (q && known[q]) setKey(known[q]);
  }, []);

  const choose = (k: PeriodKey) => {
    setKey(k);
    const url = new URL(window.location.href);
    url.searchParams.set('period', k === 'lastMonth' ? 'last-month' : k);
    window.history.replaceState(null, '', url);
  };

  const load = useLoad(`reports-${store.id}-${key}-${key === 'range' ? `${range.from}-${range.to}` : ''}`, async () => {
    const now = new Date();
    const p = period(key, now, key === 'range' ? { from: fromIso(range.from), to: fromIso(range.to) } : undefined);
    const [t, rows, prev] = await Promise.all([tills(store.id), sales(store.id, p.from, p.to), sales(store.id, p.prevFrom, p.prevTo)]);
    // Today's sales tell whether the shop is trading, for the quiet-till rule.
    const todays = p.open ? rows.filter((s) => sameDay(new Date(s.created_at), now)) : await sales(store.id, startOfDay(now), now);
    const items = await lines(rows.map((s) => s.id));
    return { now, p, tills: t, rows, prev, todays, items };
  });

  const d = load.data;
  const head = (
    <Head
      title="Reports"
      sub={d ? d.p.subtitle : undefined}
      subWide
      right={
        <span className="tools desk">
          <Chips value={key} onChange={choose} />
        </span>
      }
    />
  );

  return (
    <>
      {head}
      <div className="phone" style={{ display: 'grid', gap: 10 }}>
        <Chips value={key} onChange={choose} />
        {d && <span className="muted" style={{ fontSize: 14 }}>{d.p.subtitle}</span>}
      </div>
      {key === 'range' && (
        <div className="dates">
          <label>From<input type="date" value={range.from} max={range.to} onChange={(e) => e.target.value && setRange((r) => ({ ...r, from: e.target.value }))} /></label>
          <label>To<input type="date" value={range.to} min={range.from} max={iso(new Date())} onChange={(e) => e.target.value && setRange((r) => ({ ...r, to: e.target.value }))} /></label>
        </div>
      )}
      {load.offline && <Offline page="this report" at={load.updatedAt} />}
      {load.state === 'failed' ? (
        <Failed title={FAILED[key]} at={load.failedAt} retry={load.retry} />
      ) : !d ? (
        load.offline ? null : <ReportSkeleton />
      ) : (
        <Report data={d} symbol={symbol} updatedAt={load.updatedAt} offline={load.offline} />
      )}
    </>
  );
}

function Chips({ value, onChange }: { value: PeriodKey; onChange: (k: PeriodKey) => void }) {
  return (
    <div className="periods" role="tablist" aria-label="Period">
      {CHIPS.map((c) => (
        <button key={c.key} role="tab" aria-selected={value === c.key} className={value === c.key ? 'on' : ''} onClick={() => onChange(c.key)}>
          {c.label}
        </button>
      ))}
    </div>
  );
}

type Data = {
  now: Date; p: ReturnType<typeof period>; tills: Awaited<ReturnType<typeof tills>>;
  rows: Sale[]; prev: Sale[]; todays: Sale[]; items: Awaited<ReturnType<typeof lines>>;
};

function Report({ data, symbol, updatedAt, offline }: { data: Data; symbol: string; updatedAt: Date | null; offline: boolean }) {
  const { now, p, rows, prev, todays, items } = data;
  const total = rows.reduce((n, s) => n + s.total_amount, 0);
  const cmp = compare(total, prev.reduce((n, s) => n + s.total_amount, 0), p);
  const views = tillViews(data.tills, todays, now);
  const quiet = p.open ? views.filter((v) => v.quiet) : [];
  const missing = missingLine(quiet, now, 'period');
  const name = tillNamer(data.tills);

  const buckets =
    p.granularity === 'hour' ? byHour(rows, p.from, now)
    : p.key === 'week' ? byDay(rows, p.from, addDays(p.from, 7), now, 'weekday')
    : p.key === 'month' ? byDay(rows, p.from, new Date(p.from.getFullYear(), p.from.getMonth() + 1, 1), now, 'date')
    : byDay(rows, p.from, p.open ? addDays(startOfDay(now), 1) : p.to, now, p.key === 'range' && rows.length && buckets7(p) ? 'weekday' : 'date');
  const open = buckets.find((b) => b.state === 'open');
  const caption = open ? (p.granularity === 'hour' ? `Lighter bar: ${open.label}:00 is still going` : 'Lighter bar: today, still going') : null;

  // By till, from the till code on each receipt.
  const tillTotals = new Map<string | null, { n: number; total: number }>();
  for (const s of rows) {
    const c = tillCodeOf(s.receipt_number);
    const e = tillTotals.get(c) ?? { n: 0, total: 0 };
    e.n += 1;
    e.total += s.total_amount;
    tillTotals.set(c, e);
  }
  const byTill = [...tillTotals.entries()].sort((a, b) => b[1].total - a[1].total);
  const quietByCode = new Map(quiet.map((v) => [v.till.code, v]));

  const pay = PAYMENTS.map((m) => ({ ...m, total: rows.filter((s) => s.payment_method === m.key).reduce((n, s) => n + s.total_amount, 0) }))
    .filter((m) => m.total > 0);

  const best = new Map<string, { sold: number; taken: number }>();
  for (const l of items) {
    const e = best.get(l.product_name) ?? { sold: 0, taken: 0 };
    e.sold += l.quantity;
    e.taken += l.line_total;
    best.set(l.product_name, e);
  }
  const sellers = [...best.entries()].sort((a, b) => b[1].sold - a[1].sold || b[1].taken - a[1].taken).slice(0, 10);

  const eyebrow = offline && p.open && updatedAt ? p.eyebrow.replace('so far', `as of ${hm(updatedAt)}`) : p.eyebrow;

  if (!rows.length) {
    return (
      <div className="card empty">
        <div className="t">{p.key === 'today' ? 'No sales yet today' : p.open ? 'No sales yet' : 'No sales in these dates'}</div>
        <p>{p.open ? 'Sales show here once a till sends them.' : 'Nothing was sold in this period.'}</p>
      </div>
    );
  }

  const stats = [
    { v: rows.length.toLocaleString('en'), l: rows.length === 1 ? 'sale' : 'sales' },
    { v: money(total / rows.length, symbol), l: 'average sale' },
  ];

  return (
    <>
      <div className="reports-grid">
        <section className="card" style={{ display: 'grid', gap: 14 }}>
          <div style={{ display: 'flex', gap: 20, justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div style={{ display: 'grid', gap: 10, minWidth: 0 }}>
              <span className="eyebrow">{eyebrow}</span>
              <div style={{ display: 'flex', gap: 16, alignItems: 'center', flexWrap: 'wrap' }}>
                <span className={`amount${offline ? ' stale' : ''}`}>{money(total, symbol)}</span>
                <Compare c={cmp} stale={offline} />
              </div>
              {missing && <Unsure>{missing}</Unsure>}
            </div>
            <div className="desk"><Stats items={stats} /></div>
          </div>
          <div className="phone" style={{ display: 'grid', gap: 14 }}>
            <hr className="hair" />
            <Stats items={stats} />
          </div>
          <div className="desk" style={{ marginTop: 8 }}>
            <BarChart buckets={buckets} height={170} ticks={3} gap={buckets.length > 12 ? 4 : 18} symbol={symbol} caption={caption} />
          </div>
        </section>

        <section className="card phone">
          <span className="eyebrow">{p.granularity === 'hour' ? 'By hour' : 'By day'}</span>
          <BarChart buckets={buckets} height={130} ticks={2} gap={buckets.length > 12 ? 2 : 7} symbol={symbol} caption={caption} />
        </section>

        <div className="col">
          <section className="card">
            <span className="eyebrow">By till</span>
            <div className="rows" style={{ borderTop: '1px solid var(--hair)' }}>
              {byTill.map(([code, e]) => {
                const q = code ? quietByCode.get(code) : undefined;
                return (
                  <div key={code ?? 'none'} className="till-row">
                    <span className="n">{name(code)}</span>
                    <span className="barcell">
                      <div className="share"><i style={{ width: `${(e.total / total) * 100}%` }} /></div>
                      <div className="s desk">{salesLine(e.n, q, now)}</div>
                    </span>
                    <span className="amt r">{money(e.total, symbol)}</span>
                    <span className="s phone" style={{ gridColumn: '1 / -1', marginTop: 0 }}>{salesLine(e.n, q, now)}</span>
                  </div>
                );
              })}
            </div>
          </section>

          <section className="card">
            <span className="eyebrow">By payment</span>
            <div className="paybar" aria-hidden>
              {pay.map((m) => <i key={m.key} className={m.className} style={{ flex: m.total }} />)}
            </div>
            <div className="paylist">
              {pay.map((m) => (
                <div key={m.key}>
                  <span className={`sw ${m.className}`} />
                  {m.label}
                  <span className="phone faint" style={{ marginLeft: 'auto' }}>{Math.round((m.total / total) * 100)}%</span>
                  <span className="amt" style={{ marginLeft: undefined }}>{money(m.total, symbol)}</span>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>

      {sellers.length > 0 && (
        <section className="card">
          <table className="t desk">
            <thead>
              <tr><th /><th className="eyebrow">Best sellers</th><th className="eyebrow r">Sold</th><th className="eyebrow r" style={{ width: 140 }}>Takings</th></tr>
            </thead>
            <tbody>
              {sellers.map(([n, e], i) => (
                <tr key={n}>
                  <td className="rank">{i + 1}</td>
                  <td style={{ fontWeight: 500 }}>{n}</td>
                  <td className="r">{fmtQty(e.sold)}</td>
                  <td className="r" style={{ fontWeight: 500 }}>{money(e.taken, symbol)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="phone">
            <span className="eyebrow">Best sellers</span>
            <div className="rows" style={{ borderTop: '1px solid var(--hair)', marginTop: 10 }}>
              {sellers.map(([n, e], i) => (
                <div key={n} className="list-row">
                  <span className="faint" style={{ width: 28 }}>{i + 1}</span>
                  <span className="main"><div className="n">{n}</div><div className="m">{fmtQty(e.sold)} sold</div></span>
                  <span className="v">{money(e.taken, symbol)}</span>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}
    </>
  );
}

function buckets7(p: ReturnType<typeof period>): boolean {
  return (p.to.getTime() - p.from.getTime()) / 86_400_000 <= 7;
}

function salesLine(n: number, q: ReturnType<typeof tillViews>[number] | undefined, now: Date): string {
  const base = plural(n, 'sale', 'sales');
  return q ? `${base} · last report ${heardAt(q.lastSeen, now)}` : base;
}

function fmtQty(n: number): string {
  return Number.isInteger(n) ? n.toLocaleString('en') : n.toFixed(2);
}

function ReportSkeleton() {
  return (
    <div className="reports-grid">
      <div className="card" style={{ display: 'grid', gap: 14 }}>
        <Skeleton h={11} w={160} />
        <Skeleton h={40} w={220} />
        <Skeleton h={24} w={260} r={12} />
        <Skeleton h={150} />
      </div>
      <div className="col">
        <div className="card" style={{ display: 'grid', gap: 16 }}><Skeleton h={11} w={80} /><Skeleton h={30} /><Skeleton h={30} /></div>
        <div className="card" style={{ display: 'grid', gap: 16 }}><Skeleton h={11} w={100} /><Skeleton h={12} /><Skeleton h={60} /></div>
      </div>
    </div>
  );
}
