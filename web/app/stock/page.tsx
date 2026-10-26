'use client';

import { useEffect, useState } from 'react';
import { Shell, useStore } from '@/components/Shell';
import { ledger, stock, type Movement, type StockRow } from '@/lib/data';
import { whenSaid } from '@/lib/format';

export default function StockPage() {
  return (
    <Shell title="Stock">
      <Stock />
    </Shell>
  );
}

const REASON: Record<string, string> = {
  SALE: 'Sale', RESTOCK: 'Restock', PURCHASE: 'Delivery', MANUAL_ADJUSTMENT: 'Adjustment', CORRECTION: 'Correction', COUNT: 'Count',
};

/**
 * The store's stock as the whole ledger — every till's movements summed (CLAUDE.md § Multi-till).
 * Below zero is a signal (sold more than recorded), shown as stopped, never as an error.
 */
function Stock() {
  const store = useStore();
  const [rows, setRows] = useState<StockRow[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState<StockRow | null>(null);
  const [history, setHistory] = useState<Movement[] | null>(null);

  useEffect(() => {
    setRows(null);
    stock(store.id).then(setRows).catch((e: Error) => setError(e.message));
  }, [store.id]);

  useEffect(() => {
    if (!open) return;
    setHistory(null);
    ledger(store.id, open.productId).then(setHistory).catch(() => setHistory([]));
  }, [open, store.id]);

  if (error) return <p className="note">Couldn&apos;t load stock: {error}</p>;
  if (!rows) return <p className="note">Loading stock…</p>;
  if (rows.length === 0) return <p className="note">No products yet. Add them in the ShopMate app.</p>;

  return (
    <div className="grid" style={{ gridTemplateColumns: open ? 'minmax(0, 1fr) minmax(0, 1fr)' : '1fr', alignItems: 'start' }}>
      <table>
        <thead><tr><th>Product</th><th className="r">In stock</th><th>Last moved</th></tr></thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.productId} onClick={() => setOpen(r)} style={{ cursor: 'pointer' }}>
              <td>{r.name}</td>
              <td className="r num">{r.stock <= 0 ? <span className="stopped">{r.stock < 0 ? `${r.stock} · sold more than recorded` : 'Out'}</span> : r.stock}</td>
              <td className="num muted">{r.lastMovedAt ? whenSaid(r.lastMovedAt) : '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {open && (
        <section className="card">
          <div className="label">Why {open.name} says {open.stock}</div>
          {!history && <p className="note">Loading…</p>}
          {history && (
            <table style={{ marginTop: 8 }}>
              <tbody>
                {history.map((m, i) => (
                  <tr key={i}>
                    <td>
                      {REASON[m.reason] ?? m.reason}{m.note ? <span className="muted"> · {m.note}</span> : null}
                      <div className="muted num" style={{ fontSize: 12 }}>{whenSaid(m.created_at)}{m.actor ? ` · ${m.actor}` : ''}</div>
                    </td>
                    <td className="r num" style={{ fontWeight: 600 }}>{m.delta > 0 ? `+${m.delta}` : m.delta}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}
    </div>
  );
}
