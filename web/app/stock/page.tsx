'use client';

import { useEffect, useMemo, useState } from 'react';
import { Shell, useStore } from '@/components/Shell';
import { Chip, Empty, Failed, Head, Icon, Offline, Skeleton, Updated } from '@/components/ui';
import { movements, products, sales, tills } from '@/lib/data';
import { hm, qty, sameDay, startOfDay } from '@/lib/format';
import {
  belowZeroText, ledgerRows, outSinceText, perDay, runOutLabel, sortItems, stockItems, tillViews, type StockItem,
} from '@/lib/model';
import { useLoad } from '@/lib/useLoad';

export default function StockPage() {
  return (
    <Shell>
      <Stock />
    </Shell>
  );
}

const SECTIONS: { key: StockItem['section']; title: string }[] = [
  { key: 'out', title: 'Out of stock' },
  { key: 'soon', title: 'Runs out in the next 7 days' },
  { key: 'healthy', title: 'Healthy' },
];

/**
 * Stock (handoff 1i–1k): when do I run out? Each product's stock is the sum of every till's
 * movements. Only problems get a chip; healthy rows say how long they last, in plain words.
 */
function Stock() {
  const { store } = useStore();
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    const id = new URLSearchParams(window.location.search).get('product');
    if (id) setSelected(id);
  }, []);

  const select = (id: string | null) => {
    setSelected(id);
    const url = new URL(window.location.href);
    if (id) url.searchParams.set('product', id);
    else url.searchParams.delete('product');
    window.history.replaceState(null, '', url);
  };

  const load = useLoad(`stock-${store.id}`, async () => {
    const now = new Date();
    const [prods, ledger, t, today] = await Promise.all([
      products(store.id), movements(store.id), tills(store.id), sales(store.id, startOfDay(now), now),
    ]);
    return { now, items: stockItems(prods, ledger, now), tills: t, today };
  });

  const d = load.data;
  const items = useMemo(() => (d ? sortItems(d.items) : []), [d]);
  const shown = query.trim() ? items.filter((i) => i.product.name.toLowerCase().includes(query.trim().toLowerCase())) : items;
  const open = d && selected ? d.items.find((i) => i.product.id === selected) ?? null : null;

  useEffect(() => {
    if (!open) return;
    const esc = (e: KeyboardEvent) => e.key === 'Escape' && select(null);
    window.addEventListener('keydown', esc);
    return () => window.removeEventListener('keydown', esc);
  }, [open]);

  const count = d ? `${d.items.length.toLocaleString('en')} ${d.items.length === 1 ? 'product' : 'products'}` : undefined;

  return (
    <div className={`stock-split${open ? ' open' : ''}`}>
      <div className="stock-main">
        <Head
          title="Stock"
          sub={count}
          right={
            <span className="tools desk" style={{ flex: '0 1 400px' }}>
              <input className="search" type="search" placeholder="Search products" value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Search products" />
            </span>
          }
        />
        <input className="search phone" type="search" placeholder="Search products" value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Search products" />
        {load.offline && <Offline page="Stock" at={load.updatedAt} />}
        {load.state === 'failed' ? (
          <Failed title="Stock didn’t load" at={load.failedAt} retry={load.retry} />
        ) : !d ? (
          load.offline ? null : <div className="card" style={{ display: 'grid', gap: 18 }}>{Array.from({ length: 6 }, (_, i) => <Skeleton key={i} h={30} />)}</div>
        ) : d.items.length === 0 ? (
          <Empty title="No products yet">Add products on a till, and they show here after its next sync.</Empty>
        ) : shown.length === 0 ? (
          <p className="muted" style={{ margin: 0 }}>Nothing called “{query.trim()}”.</p>
        ) : (
          <>
            <section className="card flush stock-table desk">
              {SECTIONS.map((s, si) => {
                const rows = shown.filter((i) => i.section === s.key);
                if (!rows.length) return null;
                return (
                  <div key={s.key}>
                    <div className="sec" style={si ? { borderTop: '1px solid var(--beige)' } : undefined}>
                      <span className="eyebrow">{s.title} · {rows.length}</span>
                      {s.key === shown[0].section ? <span className="eyebrow">Stock</span> : null}
                    </div>
                    {rows.map((it) => (
                      <button key={it.product.id} className={`srow${open?.product.id === it.product.id ? ' sel' : ''}`} onClick={() => select(it.product.id)}>
                        <span className="n">{it.product.name}</span>
                        <span className="tag"><Tag item={it} /></span>
                        <span className="q">{qty(it.quantity)}</span>
                        <Meta item={it} now={d.now} />
                      </button>
                    ))}
                  </div>
                );
              })}
            </section>
            <div className="pstack phone">
              {SECTIONS.map((s) => {
                const rows = shown.filter((i) => i.section === s.key);
                if (!rows.length) return null;
                return (
                  <div key={s.key} className="pstack">
                    <span className="eyebrow" style={{ margin: '8px 8px 0' }}>{s.title} · {rows.length}</span>
                    <section className="card flush" style={{ overflow: 'hidden' }}>
                      {rows.map((it) => (
                        <button key={it.product.id} className="prow" onClick={() => select(it.product.id)}>
                          <span className="main">
                            <span className="n">{it.product.name}</span>
                            <PhoneMeta item={it} now={d.now} />
                          </span>
                          <span className="q">{qty(it.quantity)}</span>
                        </button>
                      ))}
                    </section>
                  </div>
                );
              })}
            </div>
          </>
        )}
        {d && <div className="faint desk" style={{ fontSize: 12.5 }}><Updated at={load.updatedAt} offline={load.offline} /></div>}
      </div>
      {open && d && <Detail item={open} data={d} close={() => select(null)} />}
    </div>
  );
}

function Tag({ item }: { item: StockItem }) {
  const r = item.runOut;
  if (r.kind === 'out' || r.kind === 'below') return <Chip tone="stopped">{runOutLabel(r)}</Chip>;
  if (item.section === 'soon') return <Chip tone="warning">{runOutLabel(r)}</Chip>;
  return <>{runOutLabel(r)}</>;
}

/** Desktop: the line under the name — only for out and below zero. */
function Meta({ item, now }: { item: StockItem; now: Date }) {
  const r = item.runOut;
  if (r.kind === 'out') return <span className="m">{outSinceText(r.since, now)}</span>;
  if (r.kind === 'below') return <span className="m">{belowZeroText(r.by)}</span>;
  return null;
}

function PhoneMeta({ item, now }: { item: StockItem; now: Date }) {
  const r = item.runOut;
  if (r.kind === 'out') return <span className="row2"><Chip tone="stopped">Out</Chip><span className="m">{outSinceText(r.since, now)}</span></span>;
  if (r.kind === 'below') return <><Chip tone="stopped">Below zero</Chip><span className="m">{belowZeroText(r.by)}</span></>;
  if (item.section === 'soon') return <Chip tone="warning">{runOutLabel(r)}</Chip>;
  return <span className="m">{runOutLabel(r)}</span>;
}

type Data = { now: Date; items: StockItem[]; tills: Awaited<ReturnType<typeof tills>>; today: Awaited<ReturnType<typeof sales>> };

/** One detail, two containers: a right-hand panel on desktop, a bottom sheet on phone. */
function Detail({ item, data, close }: { item: StockItem; data: Data; close: () => void }) {
  const { now } = data;
  const rows = ledgerRows(item, data.tills, now);
  const quiet = tillViews(data.tills, data.today, now).filter((v) => v.quiet);
  const r = item.runOut;
  const chip =
    r.kind === 'out' || r.kind === 'below' ? <Chip tone="stopped">{runOutLabel(r)}</Chip>
    : item.section === 'soon' ? <Chip tone="warning">{runOutLabel(r)}</Chip>
    : null;
  const basis =
    item.unitsPerDay ? `Selling ${perDay(item.unitsPerDay)} a day this week`
    : r.kind === 'out' ? outSinceText(r.since, now)
    : r.kind === 'below' ? belowZeroText(r.by)
    : runOutLabel(r);
  // A quiet till could have sold this after it last reported.
  const unsure = quiet.length > 0 && (item.soldToday || item.unitsPerDay != null) ? quiet[0] : null;
  const left = item.quantity < 0 ? `${qty(item.quantity)}` : `${item.quantity} left`;

  return (
    <>
      <div className="scrim" onClick={close} />
      <aside className="detail" aria-label={item.product.name}>
        <div className="top">
          <h2>{item.product.name}</h2>
          <button className="close" onClick={close} aria-label="Close">{Icon.close}</button>
        </div>
        <div style={{ display: 'grid', gap: 6, marginTop: -8 }}>
          <div className="left"><span className="big">{left}</span>{chip}</div>
          <div className="muted" style={{ fontSize: 14 }}>{basis}</div>
        </div>
        {unsure && (
          <div className="ubox">
            <div className="unsure" style={{ fontSize: 13.5 }}>
              {unsure.till.name} hasn’t reported since {sameDay(unsure.lastSeen, now) ? hm(unsure.lastSeen) : 'yesterday'}.
              {' '}Any it sold after that aren’t counted yet, so there may be fewer than {qty(item.quantity)}.
            </div>
          </div>
        )}
        <div>
          <span className="eyebrow">Why does it say {qty(item.quantity)}?</span>
          {rows.length === 0 ? (
            <p className="muted" style={{ fontSize: 13.5 }}>Nothing has moved yet. It was added with no stock.</p>
          ) : (
            <div className="ledger" style={{ marginTop: 6 }}>
              {rows.map((row, i) => (
                <div key={i} className="lr">
                  <span className="k">{row.kind}</span>
                  <span className="w">{row.meta}</span>
                  <span className="c">{row.change}</span>
                  <span className="tot">{qty(row.total)}</span>
                </div>
              ))}
              <div className="final"><span>Left now</span><span>{qty(item.quantity)}</span></div>
            </div>
          )}
        </div>
      </aside>
    </>
  );
}
