'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { createContext, useContext, useEffect, useState } from 'react';
import { notConfigured, supabase } from '@/lib/supabase';
import { stores, type Store } from '@/lib/data';

type ShellContext = { store: Store };
const Ctx = createContext<ShellContext | null>(null);

/** The store the owner is looking at. Only rendered once one is chosen. */
export function useStore(): Store {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useStore outside Shell');
  return ctx.store;
}

const NAV = [
  { href: '/', label: 'Today' },
  { href: '/reports/', label: 'Reports' },
  { href: '/stock/', label: 'Stock' },
  { href: '/tills/', label: 'Tills' },
];

/**
 * The dashboard's frame: the till app's Trade Green rail and white sheet. Signs the owner in
 * first (the dashboard has its own sign-in; see CLAUDE.md § Devices) and picks the store.
 */
export function Shell({ title, children }: { title: string; children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [state, setState] = useState<'loading' | 'signedOut' | 'ready' | 'noStores'>('loading');
  const [all, setAll] = useState<Store[]>([]);
  const [storeId, setStoreId] = useState<string | null>(null);

  useEffect(() => {
    if (notConfigured) return;
    supabase.auth.getSession().then(async ({ data }) => {
      if (!data.session) {
        setState('signedOut');
        router.replace('/login/');
        return;
      }
      const list = await stores();
      setAll(list);
      const saved = typeof window !== 'undefined' ? window.localStorage.getItem('shopmate.store') : null;
      const chosen = list.find((s) => s.id === saved) ?? list[0];
      setStoreId(chosen?.id ?? null);
      setState(chosen ? 'ready' : 'noStores');
    });
  }, [router]);

  if (notConfigured) {
    return <Centered>This dashboard was built without its Supabase settings. See web/.env.example.</Centered>;
  }
  if (state === 'loading' || state === 'signedOut') return <Centered>Opening your shop…</Centered>;
  if (state === 'noStores') return <Centered>This account has no shop yet. Create one in the ShopMate app first.</Centered>;

  const store = all.find((s) => s.id === storeId)!;
  return (
    <Ctx.Provider value={{ store }}>
      <div className="frame">
        <nav className="rail" aria-label="Sections">
          <div className="mark" aria-hidden>
            <img src="/mark.svg" alt="" style={{ height: 26 }} />
          </div>
          {NAV.map((n) => (
            <Link key={n.href} href={n.href} className={pathname === n.href ? 'selected' : ''}>
              {n.label}
            </Link>
          ))}
          <div className="spacer" />
          <button
            onClick={async () => {
              await supabase.auth.signOut();
              router.replace('/login/');
            }}
          >
            Sign out
          </button>
        </nav>
        <main className="sheet">
          <div className="header">
            <h1>{title}</h1>
            {all.length > 1 && (
              <select
                aria-label="Store"
                value={store.id}
                onChange={(e) => {
                  window.localStorage.setItem('shopmate.store', e.target.value);
                  setStoreId(e.target.value);
                }}
              >
                {all.map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            )}
            {all.length <= 1 && <span className="muted">{store.name}</span>}
          </div>
          {children}
        </main>
      </div>
    </Ctx.Provider>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', color: '#fff', padding: 24, textAlign: 'center' }}>
      {children}
    </div>
  );
}
