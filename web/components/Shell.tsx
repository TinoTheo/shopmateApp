'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { createContext, useContext, useEffect, useRef, useState } from 'react';
import { notConfigured, supabase } from '@/lib/supabase';
import { ownerName, stores, type Store } from '@/lib/data';
import { Icon } from './ui';

type ShellContext = { store: Store; symbol: string };
const Ctx = createContext<ShellContext | null>(null);

/** The store the owner is looking at. Every screen re-fetches when it changes. */
export function useStore(): ShellContext {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useStore outside Shell');
  return ctx;
}

const NAV = [
  { href: '/', label: 'Today' },
  { href: '/reports/', label: 'Reports' },
  { href: '/stock/', label: 'Stock' },
  { href: '/tills/', label: 'Tills' },
];

const STORE_KEY = 'shopmate.store';

function saved(): string | null {
  try { return window.localStorage.getItem(STORE_KEY); } catch { return null; }
}

/**
 * The frame (handoff § Global frame). Desktop: a 224px Trade Green sidebar and a sheet with a
 * 24px top-left corner. Phone: a green top bar, and a full-height green menu in place of the
 * sheet. Never a bottom tab bar.
 */
export function Shell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [state, setState] = useState<'loading' | 'ready' | 'noStores' | 'failed'>('loading');
  const [all, setAll] = useState<Store[]>([]);
  const [storeId, setStoreId] = useState<string | null>(null);
  const [owner, setOwner] = useState<string | null>(null);
  const [email, setEmail] = useState<string>('');
  const [menu, setMenu] = useState(false);
  const [picker, setPicker] = useState(false);
  const pickerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (notConfigured) return;
    supabase.auth.getSession().then(async ({ data }) => {
      if (!data.session) {
        router.replace('/login/');
        return;
      }
      setEmail(data.session.user.email ?? '');
      try {
        const list = await stores();
        setAll(list);
        const chosen = list.find((s) => s.id === saved()) ?? list[0];
        setStoreId(chosen?.id ?? null);
        setState(chosen ? 'ready' : 'noStores');
      } catch {
        setState('failed');
      }
    });
  }, [router]);

  useEffect(() => {
    if (!storeId) return;
    ownerName(storeId).then(setOwner).catch(() => setOwner(null));
  }, [storeId]);

  useEffect(() => setMenu(false), [pathname]);

  useEffect(() => {
    if (!picker) return;
    const away = (e: MouseEvent) => {
      if (!pickerRef.current?.contains(e.target as Node)) setPicker(false);
    };
    document.addEventListener('mousedown', away);
    return () => document.removeEventListener('mousedown', away);
  }, [picker]);

  if (notConfigured) return <Centered>This dashboard was built without its Supabase settings. See web/.env.example.</Centered>;
  if (state === 'noStores') return <Centered>This account has no shop yet. Set one up in the ShopMate app first.</Centered>;
  if (state === 'failed') return <Centered>ShopMate didn’t answer in time. Reload the page to try again.</Centered>;
  if (state === 'loading' || !storeId) return <div style={{ minHeight: '100vh' }} />;

  const store = all.find((s) => s.id === storeId)!;
  const choose = (id: string) => {
    try { window.localStorage.setItem(STORE_KEY, id); } catch { /* remembered for this visit only */ }
    setStoreId(id);
    setPicker(false);
    setMenu(false);
  };
  const signOut = async () => {
    await supabase.auth.signOut();
    router.replace('/login/');
  };
  const who = owner ?? email;
  const on = (href: string) => (href === '/' ? pathname === '/' : pathname.startsWith(href.replace(/\/$/, '')));
  const many = all.length > 1;

  const mark = (small?: boolean) => (
    <div className={`markc${small ? ' small' : ''}`} aria-hidden>
      <img src="/mark.svg" alt="" />
    </div>
  );

  return (
    <Ctx.Provider value={{ store, symbol: store.currency_symbol || '$' }}>
      <div className="frame">
        <aside className="rail" aria-label="ShopMate">
          {mark()}
          <div className="picker" ref={pickerRef}>
            <button onClick={() => setPicker((p) => !p)} disabled={!many} aria-expanded={picker} aria-haspopup="listbox">
              <span style={{ flex: 1, minWidth: 0 }}>
                <span className="lbl" style={{ display: 'block' }}>Store</span>
                <span className="name">{store.name}</span>
              </span>
              {many ? Icon.chevron : null}
            </button>
            {picker && (
              <ul role="listbox" aria-label="Store">
                {all.map((s) => (
                  <li key={s.id}>
                    <button role="option" aria-selected={s.id === store.id} onClick={() => choose(s.id)}>
                      {s.name}
                      {s.id === store.id ? Icon.check : null}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <nav className="nav" aria-label="Sections">
            {NAV.map((n) => (
              <Link key={n.href} href={n.href} className={on(n.href) ? 'on' : ''} aria-current={on(n.href) ? 'page' : undefined}>
                {n.label}
              </Link>
            ))}
          </nav>
          <div className="who">
            <div className="owner" title={email}>{who}</div>
            <button onClick={signOut}>Sign out</button>
          </div>
        </aside>

        <header className="topbar">
          {mark(true)}
          {!menu && (
            <button className="store" onClick={() => setMenu(true)} aria-label={`Store: ${store.name}`}>
              <span>{store.name}</span>
              {many ? Icon.chevron : null}
            </button>
          )}
          <button className="pillbtn" onClick={() => setMenu((m) => !m)} aria-expanded={menu}>
            {menu ? Icon.close : Icon.menu}
            {menu ? 'Close' : 'Menu'}
          </button>
        </header>

        {menu ? (
          <div className="menu">
            <div className="eyebrow">Go to</div>
            <nav className="nav" aria-label="Sections">
              {NAV.map((n) => (
                <Link key={n.href} href={n.href} className={on(n.href) ? 'on' : ''} onClick={() => setMenu(false)}>
                  {n.label}
                </Link>
              ))}
            </nav>
            <div className="eyebrow" style={{ marginTop: 20 }}>Store</div>
            <div className="stores">
              {all.map((s) => (
                <button key={s.id} className={s.id === store.id ? 'cur' : ''} onClick={() => choose(s.id)}>
                  {s.name}
                  {s.id === store.id ? Icon.check : null}
                </button>
              ))}
            </div>
            <div className="who">
              <div className="owner">{who}</div>
              <button onClick={signOut}>Sign out</button>
            </div>
          </div>
        ) : (
          <main className="sheet" key={store.id}>{children}</main>
        )}
      </div>
    </Ctx.Provider>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', color: '#fff', padding: 24, textAlign: 'center', fontSize: 15 }}>
      {children}
    </div>
  );
}
