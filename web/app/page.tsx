'use client';

import { useEffect, useState } from 'react';
import { Shell, useStore } from '@/components/Shell';
import { byTill, sales, tills, type Sale, type Till } from '@/lib/data';
import { money, plural, whenSaid } from '@/lib/format';

export default function TodayPage() {
  return (
    <Shell title="Today">
      <Today />
    </Shell>
  );
}

/** Today across every till: what came in, and when each till last checked in. */
function Today() {
  const store = useStore();
  const [rows, setRows] = useState<Sale[] | null>(null);
  const [tillList, setTills] = useState<Till[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    const end = new Date(start);
    end.setDate(end.getDate() + 1);
    setRows(null);
    Promise.all([sales(store.id, start, end), tills(store.id)])
      .then(([s, t]) => {
        setRows(s);
        setTills(t);
      })
      .catch((e: Error) => setError(e.message));
  }, [store.id]);

  if (error) return <p className="note">Couldn&apos;t load today: {error}</p>;
  if (!rows) return <p className="note">Loading today…</p>;

  const total = rows.reduce((a, s) => a + s.total_amount, 0);
  const perTill = byTill(rows);
  const now = Date.now();

  return (
    <>
      <div className="grid">
        <div className="card">
          <div className="label">Sales today, so far</div>
          <div className="value num">{money(total, store.currency_symbol)}</div>
          <div className="sub num">{plural(rows.length, 'sale', 'sales')}</div>
        </div>
        <div className="card">
          <div className="label">Average sale</div>
          <div className="value num">{rows.length ? money(total / rows.length, store.currency_symbol) : '—'}</div>
          <div className="sub">across all tills</div>
        </div>
      </div>

      <div className="eyebrow">Tills</div>
      {tillList.length === 0 ? (
        <p className="note">No tills yet. On the device you&apos;ll sell on, choose &ldquo;Yes, sell here&rdquo;.</p>
      ) : (
        <table>
          <thead>
            <tr><th>Till</th><th>Last heard from</th><th className="r">Sales today</th><th className="r">Taken today</th></tr>
          </thead>
          <tbody>
            {tillList.map((t) => {
              const today = perTill.get(t.code);
              // Quiet for over an hour: its sales may not be in today's figure yet. Say so.
              const quiet = now - new Date(t.last_seen_at).getTime() > 60 * 60 * 1000;
              return (
                <tr key={t.id} className={quiet ? 'warn' : ''}>
                  <td>{t.name} <span className="muted num">· {t.code}</span></td>
                  <td className="num">{whenSaid(t.last_seen_at)}{quiet && <span className="muted"> · sales since may not be in yet</span>}</td>
                  <td className="r num">{today?.count ?? 0}</td>
                  <td className="r num">{money(today?.total ?? 0, store.currency_symbol)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </>
  );
}
