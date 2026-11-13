'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { DASHBOARD_URL } from '@/lib/site';

const LINKS = [
  { href: '/#how', label: 'How it works' },
  { href: '/#stores', label: 'Who it’s for' },
  { href: '/pricing/', label: 'Pricing' },
];

export function Header() {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();
  useEffect(() => setOpen(false), [pathname]);

  return (
    <header className="header">
      <div className="wrap header-row">
        <Link href="/" className="brand" aria-label="ShopMate home">
          <img src="/lockup.svg" alt="ShopMate" />
        </Link>
        <nav className={`nav${open ? ' open' : ''}`} aria-label="Main">
          {LINKS.map((l) => (
            <Link key={l.href} href={l.href} onClick={() => setOpen(false)}>{l.label}</Link>
          ))}
          <a href={DASHBOARD_URL} className="signin">Sign in</a>
        </nav>
        <Link href="/walkthrough/" className="btn btn-neon header-cta">Book a walkthrough</Link>
        <button className="menu-btn" aria-expanded={open} aria-label={open ? 'Close menu' : 'Open menu'} onClick={() => setOpen((o) => !o)}>
          <svg width="20" height="20" viewBox="0 0 20 20" aria-hidden>
            {open
              ? <path d="M5 5l10 10M15 5L5 15" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
              : <path d="M3 6h14M3 10h14M3 14h14" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />}
          </svg>
        </button>
      </div>
    </header>
  );
}
