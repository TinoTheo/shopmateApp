'use client';

import { useEffect, useId, useRef, useState } from 'react';

// "Ink over" (handoff § Lockup animation): the neon M writes itself over the white M along its
// centreline, the M moves into its slot, "Shop" and "ate" settle, the tagline arrives last.
// Plays once; clicking replays it. Reduced motion: the whole lockup fades in over 200ms.

/** The M's centreline — a mask only, never drawn. Lockup units: 1200 × 945. */
const CL = 'M33 707 L33 390 A91 91 0 0 1 215 390 L215 615 A45.5 64 0 0 0 306 615 L306 563 A45.5 45 0 0 1 397 563 L397 790';
const MC = [215.5, 544.5];
const LH = 945;
const TOTAL = 1550;

function bezier(a: number, b: number, c: number, d: number) {
  return (t: number) => {
    if (t <= 0) return 0;
    if (t >= 1) return 1;
    let s = t;
    for (let i = 0; i < 10; i++) {
      const x = 3 * a * (1 - s) * (1 - s) * s + 3 * c * (1 - s) * s * s + s * s * s - t;
      const dx = 3 * a * (1 - s) * (1 - s) + 6 * (c - a) * (1 - s) * s + 3 * (1 - c) * s * s;
      if (Math.abs(dx) < 1e-6) break;
      s = Math.min(1, Math.max(0, s - x / dx));
    }
    return 3 * b * (1 - s) * (1 - s) * s + 3 * d * (1 - s) * s * s + s * s * s;
  };
}
const OUT = bezier(0, 0, 0.2, 1);
const STD = bezier(0.4, 0, 0.2, 1);
const seg = (t: number, start: number, dur: number, ease?: (x: number) => number) => {
  const p = Math.min(1, Math.max(0, (t - start) / dur));
  return ease ? ease(p) : p;
};

export function Lockup({ width, delay = 400 }: { width: number; delay?: number }) {
  const [t, setT] = useState(0);
  const [reduced, setReduced] = useState(false);
  const raf = useRef(0);
  const maskId = `lk${useId().replace(/[^a-zA-Z0-9]/g, '')}m`;

  const play = () => {
    cancelAnimationFrame(raf.current);
    const r = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
    setReduced(r);
    const end = r ? 200 : TOTAL;
    const t0 = performance.now() + delay;
    setT(0);
    const loop = (now: number) => {
      const e = Math.max(0, now - t0);
      setT(Math.min(e, end));
      if (e < end) raf.current = requestAnimationFrame(loop);
    };
    raf.current = requestAnimationFrame(loop);
  };

  useEffect(() => {
    play();
    return () => cancelAnimationFrame(raf.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const k = width / 1200;
  const H = LH * k;
  const s = reduced
    ? (() => {
        const p = seg(t, 0, 200);
        return { move: 1, trace: null as number | null, neon: p, white: false, shop: p, sTy: 0, ate: p, aTy: 0, tag: p, fadeWhite: 1 - p };
      })()
    : (() => {
        const tr = seg(t, 0, 700, OUT);
        const sh = seg(t, 850, 300, OUT);
        const at = seg(t, 950, 300, OUT);
        return {
          move: seg(t, 550, 500, STD), trace: tr as number | null, neon: tr > 0 ? 1 : 0, white: t < 700,
          shop: sh, sTy: 8 * (1 - sh), ate: at, aTy: 8 * (1 - at), tag: seg(t, 1250, 300, OUT), fadeWhite: 0,
        };
      })();

  // The M starts at the centre of the lockup box and travels to its slot.
  const mt = (p: number) => {
    const cx = width / 2 + (MC[0] * k - width / 2) * p;
    const cy = H / 2 + (MC[1] * k - H / 2) * p;
    return `translate(${cx} ${cy}) scale(${k}) translate(${-MC[0]} ${-MC[1]})`;
  };
  const layer = (href: string, extra: React.SVGProps<SVGImageElement> = {}) => (
    <image href={href} x={0} y={0} width={1200} height={823} {...extra} />
  );

  return (
    <svg
      viewBox={`0 0 ${width} ${H}`}
      width={width}
      height={H}
      role="img"
      aria-label="ShopMate, uncompromising control"
      onClick={play}
      style={{ display: 'block', overflow: 'visible', cursor: 'pointer', maxWidth: '100%', height: 'auto' }}
    >
      <defs>
        {s.trace != null && (
          <mask id={maskId} maskUnits="userSpaceOnUse" x={-100} y={0} width={700} height={1000}>
            <path
              d={CL} fill="none" stroke="#fff" strokeWidth={86} strokeLinecap="round" strokeLinejoin="round"
              pathLength={1} strokeDasharray="1 1" strokeDashoffset={1 - s.trace}
            />
          </mask>
        )}
      </defs>
      <g transform={`scale(${k})`}>
        {layer('/anim/shop.png', { opacity: s.shop, transform: `translate(0 ${s.sTy / k})` })}
        {layer('/anim/ate.png', { opacity: s.ate, transform: `translate(0 ${s.aTy / k})` })}
        <image href="/anim/tagline.svg" x={525} y={868} width={620} height={73} opacity={s.tag} />
      </g>
      {s.fadeWhite > 0 && <g transform={mt(0)}>{layer('/anim/m-white.png', { opacity: s.fadeWhite })}</g>}
      <g transform={mt(s.move)}>
        {s.white && layer('/anim/m-white.png')}
        {s.neon > 0 && layer('/anim/m-neon.png', { opacity: s.neon, mask: s.trace != null ? `url(#${maskId})` : undefined })}
      </g>
    </svg>
  );
}
