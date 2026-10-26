'use client';

import { useEffect, useState } from 'react';
import { Shell, useStore } from '@/components/Shell';
import { tills, type Till } from '@/lib/data';
import { supabase } from '@/lib/supabase';
import { whenSaid } from '@/lib/format';

export default function TillsPage() {
  return (
    <Shell title="Tills">
      <Tills />
    </Shell>
  );
}

/** The store's tills (CLAUDE.md § Devices). Removing one stops it at its next sync. */
function Tills() {
  const store = useStore();
  const [rows, setRows] = useState<Till[] | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const load = () => tills(store.id).then(setRows).catch((e: Error) => setMessage(e.message));
  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [store.id]);

  async function remove(t: Till) {
    if (!window.confirm(`Remove ${t.name}? It stops taking and sending sales at its next sync. Anything it hasn't sent stays on it.`)) return;
    const { error } = await supabase.from('tills').update({ revoked_at: new Date().toISOString() }).eq('id', t.id);
    setMessage(error ? `Couldn't remove ${t.name}: ${error.message}` : `${t.name} is removed. It stops at its next sync.`);
    load();
  }

  if (!rows) return <p className="note">Loading tills…</p>;
  return (
    <>
      {rows.length === 0 && <p className="note">No tills yet. On the device you&apos;ll sell on, choose &ldquo;Yes, sell here&rdquo;.</p>}
      {rows.length > 0 && (
        <table>
          <thead><tr><th>Till</th><th>Code</th><th>Last heard from</th><th /></tr></thead>
          <tbody>
            {rows.map((t) => (
              <tr key={t.id}>
                <td>{t.name}</td>
                <td className="num">{t.code}</td>
                <td className="num">{whenSaid(t.last_seen_at)}</td>
                <td className="r"><button className="chips" style={{ border: 0, background: 'none', color: 'var(--ink-secondary)', cursor: 'pointer', font: 'inherit' }} onClick={() => remove(t)}>Remove</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {message && <p className="note">{message}</p>}
    </>
  );
}
