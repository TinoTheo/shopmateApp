'use client';

import type { ReactNode } from 'react';
import { hm } from '@/lib/format';
import { axis, type Bucket, type Comparison } from '@/lib/model';
import { moneyShort } from '@/lib/format';

// The component sheet (handoff 1r). Every screen builds from these.

export function Head({ title, sub, subWide, right }: { title: string; sub?: ReactNode; subWide?: boolean; right?: ReactNode }) {
  return (
    <div className="head">
      <h1>{title}</h1>
      {sub ? <span className={`sub${subWide ? ' wide' : ''}`}>{sub}</span> : null}
      {right}
    </div>
  );
}

/** "Updated 15:29", or "As of 15:02" while offline. */
export function Updated({ at, offline }: { at: Date | null; offline: boolean }) {
  if (!at) return null;
  return <span className="upd">{offline ? `As of ${hm(at)}` : `Updated ${hm(at)}`}</span>;
}

export function Chip({ tone, children }: { tone: 'warning' | 'stopped'; children: ReactNode }) {
  return <span className={`chip ${tone}`}>{children}</span>;
}

export function Compare({ c, stale }: { c: Comparison; stale?: boolean }) {
  const arrow = c.dir === 'up' ? '↑ ' : c.dir === 'down' ? '↓ ' : '';
  return <span className={`cmp ${stale ? 'stale' : c.dir}`}>{arrow}{c.text}</span>;
}

/** The hollow marker: this value is incomplete or not yet known. */
export function Unsure({ children }: { children: ReactNode }) {
  return <div className="unsure">{children}</div>;
}

export function Stats({ items }: { items: { v: ReactNode; l: string }[] }) {
  return (
    <div className="stats">
      {items.map((s) => (
        <div key={s.l}>
          <div className="v">{s.v}</div>
          <div className="l">{s.l}</div>
        </div>
      ))}
    </div>
  );
}

/**
 * Bars by hour or day. The open hour or day is drawn lighter, so it isn't read as a slow one;
 * days still to come are dashed stubs.
 */
export function BarChart({ buckets, height, ticks, gap, symbol, caption }: {
  buckets: Bucket[]; height: number; ticks: number; gap: number; symbol: string; caption?: string | null;
}) {
  const max = Math.max(0, ...buckets.map((b) => b.value));
  const steps = axis(max, ticks);
  const top = steps[steps.length - 1] || 1;
  const every = buckets.length > 16 ? Math.ceil(buckets.length / 8) : 1;
  return (
    <div>
      <div className="chart">
        <div className="y" style={{ height }}>
          {steps.map((s) => <span key={s} style={{ lineHeight: 0 }}>{moneyShort(s, symbol)}</span>)}
        </div>
        <div className="plot" style={{ height, ['--gap' as string]: `${gap}px` }}>
          <div className="grid">{steps.map((s) => <i key={s} />)}</div>
          {buckets.map((b, i) => (
            <div
              key={i}
              className={`bar ${b.state}`}
              title={b.state === 'future' ? undefined : `${b.label}: ${symbol}${b.value.toFixed(2)}`}
              style={{ height: b.state === 'future' ? undefined : `${(b.value / top) * 100}%` }}
            />
          ))}
        </div>
        <div className="x" style={{ ['--gap' as string]: `${gap}px` }}>
          {buckets.map((b, i) => (
            <span key={i}>{i % every === 0 || i === buckets.length - 1 ? b.label : ''}</span>
          ))}
        </div>
      </div>
      {caption ? <div className="chart-cap">{caption}</div> : null}
    </div>
  );
}

/** Shaped like the content it stands in for. No shimmer, no spinner. */
export function Skeleton({ h, w = '100%', r = 8 }: { h: number; w?: number | string; r?: number }) {
  return <div className="skel" style={{ height: h, width: w, borderRadius: r }} />;
}

export function SkeletonCards({ rows = 4 }: { rows?: number }) {
  return (
    <>
      <div className="card" style={{ display: 'grid', gap: 14 }}>
        <Skeleton h={11} w={140} />
        <Skeleton h={40} w={200} />
        <Skeleton h={24} w={240} r={12} />
        <Skeleton h={1} />
        <Skeleton h={36} w={260} />
      </div>
      <div className="card" style={{ display: 'grid', gap: 18 }}>
        {Array.from({ length: rows }, (_, i) => <Skeleton key={i} h={34} />)}
      </div>
    </>
  );
}

export function Failed({ title, at, retry }: { title: string; at: Date | null; retry: () => void }) {
  return (
    <div className="card failed">
      <div className="t">{title}</div>
      <p>ShopMate didn’t answer in time. Your tills keep selling and sending as normal; only this page is affected.</p>
      <button className="outline" onClick={retry}>Try again</button>
      {at ? <div className="faint" style={{ fontSize: 12.5, marginTop: 12 }}>Tried at {hm(at)}</div> : null}
    </div>
  );
}

/** Offline is a state, not an error: calm, specific, no red. */
export function Offline({ page, at }: { page: string; at: Date | null }) {
  const device = typeof window !== 'undefined' && window.innerWidth < 1024 ? 'This phone' : 'This computer';
  return (
    <div className="offline">
      <span>
        <b>{device} is offline.</b>{' '}
        {at
          ? `You’re seeing ${page} as it was at ${hm(at)}. It updates by itself when you’re back online.`
          : `${page} shows here when you’re back online.`}
      </span>
    </div>
  );
}

export function Empty({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="card empty">
      <div className="t">{title}</div>
      <p>{children}</p>
    </div>
  );
}

export function Dialog({ title, body, confirm, cancel, onConfirm, onCancel, busy }: {
  title: string; body: string; confirm: string; cancel: string; onConfirm: () => void; onCancel: () => void; busy?: boolean;
}) {
  return (
    <div className="dialog-scrim" onClick={onCancel}>
      <div className="dialog" role="dialog" aria-modal="true" aria-label={title} onClick={(e) => e.stopPropagation()}>
        <h2>{title}</h2>
        <p>{body}</p>
        <div className="acts">
          <button className="btn ghost" onClick={onCancel} autoFocus>{cancel}</button>
          <button className="btn" onClick={onConfirm} disabled={busy}>{confirm}</button>
        </div>
      </div>
    </div>
  );
}

export const Icon = {
  chevron: (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden>
      <path d="M4 6l4 4 4-4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ),
  menu: (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden>
      <path d="M2 4.5h14M2 9h14M2 13.5h14" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  ),
  close: (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden>
      <path d="M3.5 3.5l9 9M12.5 3.5l-9 9" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  ),
  check: (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden>
      <path d="M3.5 9.5l3.5 3.5 7.5-8" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ),
  tick: (
    <svg width="10" height="10" viewBox="0 0 10 10" fill="none" aria-hidden>
      <path d="M2.2 5.2l1.8 1.8 3.8-4" stroke="#000" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ),
};
