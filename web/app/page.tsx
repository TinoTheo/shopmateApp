'use client';

import Link from 'next/link';
import { Shell, useStore } from '@/components/Shell';
import { BarChart, Chip, Compare, Empty, Failed, Head, Offline, SkeletonCards, Stats, Unsure, Updated } from '@/components/ui';
import { movements, products, sales, tills } from '@/lib/data';
import { heardAt, hm, longDate, money, plural, sameDay } from '@/lib/format';
import { attention, byHour, compare, missingLine, period, stockItems, tillViews } from '@/lib/model';
import { useLoad } from '@/lib/useLoad';

export default function TodayPage() {
  return (
    <Shell>
      <Today />
    </Shell>
  );
}

/**
 * Today (handoff 1d, 1e): is my shop OK right now? The total and how it compares, why it might be
 * low, then what needs you — anything that makes the numbers uncertain first, then stopped, then
 * warning — then the tills.
 */
function Today() {
  const { store, symbol } = useStore();
  const load = useLoad(`today-${store.id}`, async () => {
    const now = new Date();
    const p = period('today', now);
    const [t, today, prev, prods, ledger] = await Promise.all([
      tills(store.id), sales(store.id, p.from, now), sales(store.id, p.prevFrom, p.prevTo), products(store.id), movements(store.id),
    ]);
    return { now, p, tills: t, today, prev, prods, ledger };
  });

  const head = (
    <Head
      title="Today"
      sub={load.data ? `${longDate(load.data.now)}, so far` : undefined}
      subWide
      right={<Updated at={load.updatedAt} offline={load.offline} />}
    />
  );

  if (load.state === 'failed') return <>{head}<Failed title="Today didn’t load" at={load.failedAt} retry={load.retry} /></>;
  if (!load.data) {
    return <>{head}{load.offline ? <Offline page="Today" at={null} /> : <div className="today-grid"><div className="col"><SkeletonCards /></div></div>}</>;
  }

  const { now, p, today, prev, prods, ledger } = load.data;
  const stale = load.offline;
  const asOf = load.updatedAt ? hm(load.updatedAt) : hm(now);
  const views = tillViews(load.data.tills, today, now);
  const active = views.filter((v) => !v.removed);
  const quiet = active.filter((v) => v.quiet);
  const total = today.reduce((n, s) => n + s.total_amount, 0);
  const cmp = compare(total, prev.reduce((n, s) => n + s.total_amount, 0), p);
  const items = stockItems(prods, ledger, now);
  const needs = attention(views, items, symbol, now, stale);
  const hours = byHour(today, now, now);
  const openHour = hours.find((h) => h.state === 'open');
  const missing = missingLine(quiet, now, 'today');

  return (
    <>
      {head}
      {stale && <Offline page="Today" at={load.updatedAt} />}
      <div className="today-grid">
        <div className="col">
          <section className="card" style={{ display: 'grid', gap: 14 }}>
            <span className="eyebrow">{stale ? `Sales today, as of ${asOf}` : 'Sales today, so far'}</span>
            <div style={{ display: 'flex', gap: 16, alignItems: 'center', flexWrap: 'wrap' }}>
              <span className={`amount${stale ? ' stale' : ''}`}>{money(total, symbol)}</span>
              {today.length > 0 && <Compare c={cmp} stale={stale} />}
            </div>
            {stale && <Unsure>Sales since {asOf} aren’t shown.</Unsure>}
            {missing && <Unsure>{missing}</Unsure>}
            <hr className="hair" />
            {today.length > 0 ? (
              <Stats
                items={[
                  { v: today.length.toLocaleString('en'), l: today.length === 1 ? 'sale' : 'sales' },
                  { v: money(total / today.length, symbol), l: 'average sale' },
                  ...(active.length ? [{ v: `${active.length - quiet.length} of ${active.length}`, l: 'tills reporting' }] : []),
                ]}
              />
            ) : (
              <div className="empty">
                <div className="t" style={{ fontSize: 15 }}>No sales yet today</div>
                <p>{noSalesBody(active, now)}</p>
              </div>
            )}
            {today.length > 0 && (
              <div className="desk" style={{ marginTop: 6 }}>
                <BarChart
                  buckets={hours} height={110} ticks={2} gap={8} symbol={symbol}
                  caption={openHour ? `Lighter bar: ${openHour.label}:00 is still going` : null}
                />
              </div>
            )}
          </section>
        </div>

        <div className="col">
          {needs.length > 0 && (
            <>
              <div className="eyebrow phone" style={{ margin: '6px 8px 0' }}>{stale ? `Needs you, as of ${asOf}` : `Needs you · ${needs.length}`}</div>
              <section className="card flush">
                <span className="eyebrow desk">{stale ? `Needs you, as of ${asOf}` : `Needs you · ${needs.length}`}</span>
                <div className="rows" style={{ borderTop: '1px solid var(--hair)' }}>
                  {needs.map((n) => (
                    <Link key={n.key} href={n.href} className="need">
                      <span className="t"><Chip tone={n.tone}>{n.chip}</Chip>{n.title}</span>
                      <span className="b">{n.body}</span>
                      <span className="link">{n.action}</span>
                    </Link>
                  ))}
                </div>
              </section>
            </>
          )}

          {active.length > 0 ? (
            <section className="card flush desk">
              <span className="eyebrow">Tills today</span>
              <div className="rows" style={{ borderTop: '1px solid var(--hair)' }}>
                {active.map((v) => (
                  <Link key={v.till.id} href={`/tills/#till-${v.till.id}`} className="need" style={{ gridTemplateColumns: '1fr auto auto', alignItems: 'center' }}>
                    <span>
                      <span style={{ fontSize: 14.5, fontWeight: 500, display: 'block' }}>{v.till.name}</span>
                      <span className="b" style={{ display: 'block' }}>Last heard {heardAt(v.lastSeen, now).replace(/^today /, '')}</span>
                    </span>
                    <span>{v.quiet ? <Chip tone="warning">Quiet {v.quietFor}</Chip> : null}</span>
                    <span className="r">
                      <span style={{ fontSize: 14.5, fontWeight: 500, display: 'block' }}>{money(v.takenToday, symbol)}</span>
                      <span className="b" style={{ display: 'block' }}>
                        {plural(v.salesToday, 'sale', 'sales')}{v.quiet && sameDay(v.lastSeen, now) ? ` to ${hm(v.lastSeen)}` : ''}
                      </span>
                    </span>
                  </Link>
                ))}
              </div>
            </section>
          ) : (
            <div className="desk">
              <Empty title="No tills yet">On the device you’ll sell on, choose ‘Yes, sell here’. It shows up here after its first sync.</Empty>
            </div>
          )}
        </div>
      </div>
    </>
  );
}

/** "All 3 tills have reported since 07:00…" when every till has checked in today. */
function noSalesBody(active: ReturnType<typeof tillViews>, now: Date): string {
  const tail = 'The first sale shows here once its till sends it.';
  if (!active.length) return tail;
  const seenToday = active.filter((v) => sameDay(v.lastSeen, now));
  if (seenToday.length !== active.length) return tail;
  const earliest = new Date(Math.min(...seenToday.map((v) => v.lastSeen.getTime())));
  const all = active.length === 1 ? 'Your till has' : `All ${active.length} tills have`;
  return `${all} reported since ${hm(earliest)}. ${tail}`;
}
