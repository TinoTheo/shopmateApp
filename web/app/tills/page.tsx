'use client';

import { useEffect, useState } from 'react';
import { Shell, useStore } from '@/components/Shell';
import { Chip, Dialog, Empty, Failed, Head, Icon, Offline, Skeleton, Updated } from '@/components/ui';
import { removeTill, sales, tills } from '@/lib/data';
import { heardAt, hm, money, sameDay, startOfDay, whenShort } from '@/lib/format';
import { tillViews, type TillView } from '@/lib/model';
import { useLoad } from '@/lib/useLoad';

export default function TillsPage() {
  return (
    <Shell>
      <Tills />
    </Shell>
  );
}

/** A removed till stays listed until it has stopped, then for a month after. */
const REMOVED_SHOWN_MS = 30 * 86_400_000;

/**
 * Tills (handoff 1l, 1m). Today's takings per till, so a quiet one stands out. Removing is two
 * steps: sent (it stops the next time it syncs), then confirmed when the till has stopped — the
 * one completed commit that gets Checkout Neon.
 */
function Tills() {
  const { store, symbol } = useStore();
  const [confirm, setConfirm] = useState<TillView | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useLoad(`tills-${store.id}`, async () => {
    const now = new Date();
    const [t, today] = await Promise.all([tills(store.id), sales(store.id, startOfDay(now), now)]);
    return { now, views: tillViews(t, today, now) };
  });

  // Scroll to "See Till 2" from Today once the rows exist.
  useEffect(() => {
    if (!load.data || !window.location.hash) return;
    document.getElementById(window.location.hash.slice(1))?.scrollIntoView({ block: 'center' });
  }, [load.data]);

  async function remove() {
    if (!confirm) return;
    setBusy(true);
    setError(null);
    try {
      await removeTill(confirm.till.id);
      setConfirm(null);
      load.retry();
    } catch {
      setError(`${confirm.till.name} wasn’t removed. ShopMate didn’t answer; try again.`);
    } finally {
      setBusy(false);
    }
  }

  const d = load.data;
  const active = d ? d.views.filter((v) => !v.removed) : [];
  const removed = d
    ? d.views.filter((v) => v.removed && (!v.till.stopped_at || d.now.getTime() - new Date(v.till.stopped_at).getTime() < REMOVED_SHOWN_MS))
    : [];
  const quiet = active.filter((v) => v.quiet);

  return (
    <>
      <Head
        title="Tills"
        sub={d ? (<><span className="desk">{active.length} selling at {store.name}</span><span className="phone">{active.length} selling</span></>) : undefined}
        right={<Updated at={load.updatedAt} offline={load.offline} />}
      />
      {load.offline && <Offline page="Tills" at={load.updatedAt} />}
      {error && <div className="banner" style={{ background: 'var(--fill)', color: 'var(--ink-2)' }}>{error}</div>}
      {load.state === 'failed' ? (
        <Failed title="Tills didn’t load" at={load.failedAt} retry={load.retry} />
      ) : !d ? (
        load.offline ? null : <div className="card" style={{ display: 'grid', gap: 18 }}>{Array.from({ length: 3 }, (_, i) => <Skeleton key={i} h={36} />)}</div>
      ) : (
        <>
          {active.length === 0 ? (
            <Empty title="No tills yet">On the device you’ll sell on, choose ‘Yes, sell here’. It shows up here after its first sync.</Empty>
          ) : (
            <>
              <section className="card flush desk">
                <table className="tills-table">
                  <thead>
                    <tr>
                      <th className="eyebrow">Till</th><th className="eyebrow">Code</th><th className="eyebrow">Last heard from</th>
                      <th className="eyebrow r">Today</th><th />
                    </tr>
                  </thead>
                  <tbody>
                    {active.map((v) => (
                      <tr key={v.till.id} id={`till-${v.till.id}`}>
                        <td style={{ fontWeight: 500 }}>{v.till.name}</td>
                        <td className="muted">{v.till.code}</td>
                        <td>
                          <span className="state">{heardAt(v.lastSeen, d.now)}{v.quiet ? <Chip tone="warning">Quiet {v.quietFor}</Chip> : null}</span>
                        </td>
                        <td className="r" style={{ fontWeight: 500 }}>
                          {money(v.takenToday, symbol)}{v.quiet && sameDay(v.lastSeen, d.now) ? ` to ${hm(v.lastSeen)}` : ''}
                        </td>
                        <td className="r"><button className="link" onClick={() => setConfirm(v)}>Remove</button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </section>
              {quiet.map((v) => (
                <div key={v.till.id} className="banner desk">
                  {v.till.name}’s sales after {sameDay(v.lastSeen, d.now) ? hm(v.lastSeen) : whenShort(v.lastSeen, d.now)} haven’t arrived. Check that it’s switched on and has data or Wi‑Fi. Today’s total doesn’t include them until it reports.
                </div>
              ))}

              <section className="card flush phone">
                <div className="rows">
                  {active.map((v) => (
                    <div key={v.till.id} id={`till-${v.till.id}`} className="trow">
                      <div className="top">
                        <span className="n">{v.till.name}</span>
                        <button className="link" style={{ fontSize: 15 }} onClick={() => setConfirm(v)}>Remove</button>
                      </div>
                      <span className="m">Code {v.till.code} · Last heard {heardAt(v.lastSeen, d.now)}</span>
                      {v.quiet && (
                        <>
                          <span><Chip tone="warning">Quiet {v.quietFor}</Chip></span>
                          <span className="m">
                            Sales after {sameDay(v.lastSeen, d.now) ? hm(v.lastSeen) : whenShort(v.lastSeen, d.now)} haven’t arrived. Check that it’s switched on and has data or Wi‑Fi.
                          </span>
                        </>
                      )}
                    </div>
                  ))}
                </div>
              </section>
            </>
          )}

          {removed.length > 0 && (
            <>
              <span className="eyebrow" style={{ marginTop: 8 }}>Removed</span>
              <section className="card flush desk">
                <table className="tills-table">
                  <tbody>
                    {removed.map((v) => (
                      <tr key={v.till.id}>
                        <td style={{ fontWeight: 500, borderTop: 0 }}>{v.till.name}</td>
                        <td className="muted" style={{ borderTop: 0 }}>{v.till.code}</td>
                        <td style={{ borderTop: 0 }}>{whenShort(v.lastSeen, d.now).replace(/^(\d\d:\d\d)$/, 'Today $1')}</td>
                        <td style={{ borderTop: 0 }}><Removal v={v} now={d.now} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </section>
              <section className="card flush phone">
                <div className="rows">
                  {removed.map((v) => (
                    <div key={v.till.id} className="trow">
                      <span className="n">{v.till.name}</span>
                      <Removal v={v} now={d.now} long />
                    </div>
                  ))}
                </div>
              </section>
            </>
          )}
          <p className="muted" style={{ fontSize: 14, margin: '4px 0 0' }}>
            To add a till, open ShopMate on the device you’ll sell on and choose ‘Yes, sell here’.
          </p>
        </>
      )}

      {confirm && (
        <Dialog
          title={`Remove ${confirm.till.name}?`}
          body="It stops selling the next time it syncs. Sales it has already sent stay in your reports."
          confirm="Remove till"
          cancel="Keep it"
          busy={busy}
          onConfirm={remove}
          onCancel={() => setConfirm(null)}
        />
      )}
    </>
  );
}

/** Sent, not yet confirmed: hollow marker. Confirmed stopped: the neon tick — a completed commit. */
function Removal({ v, now, long }: { v: TillView; now: Date; long?: boolean }) {
  const stopped = v.till.stopped_at ? new Date(v.till.stopped_at) : null;
  if (stopped) {
    return (
      <span className="state">
        <span className="tick">{Icon.tick}</span>
        <span className={long ? 'm' : undefined} style={long ? { color: 'var(--ink)' } : undefined}>
          Stopped at {sameDay(stopped, now) ? hm(stopped) : whenShort(stopped, now)}{long ? '.' : ''}
        </span>
      </span>
    );
  }
  return (
    <span className="state" style={{ alignItems: 'baseline' }}>
      <span className="hollow" />
      <span className={long ? 'm' : 'muted'}>
        {long ? `Stops the next time it syncs. Not heard from since ${whenShort(v.lastSeen, now)}.` : 'Stops at its next sync'}
      </span>
    </span>
  );
}
