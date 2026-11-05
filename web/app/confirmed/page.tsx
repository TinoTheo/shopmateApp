'use client';

import { useEffect, useState } from 'react';
import { Lockup } from '@/components/Lockup';

/**
 * Where the sign-up confirmation email's link lands (the app sends people here). Supabase has
 * confirmed the address by the time this page opens; an expired or used link arrives with an
 * error in the address instead.
 */
export default function ConfirmedPage() {
  const [state, setState] = useState<'checking' | 'confirmed' | 'expired'>('checking');

  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.slice(1) || window.location.search.slice(1));
    if (params.get('error') || params.get('error_code')) return setState('expired');
    setState('confirmed');
  }, []);

  return (
    <div className="signin">
      <div className="brand">
        <div className="stage"><Lockup width={220} delay={200} /></div>
      </div>
      <div className="form-sheet">
        <div style={{ width: '100%', maxWidth: 360, display: 'flex', flexDirection: 'column', gap: 14 }}>
          {state === 'expired' ? (
            <>
              <h1 style={{ margin: 0, fontSize: 26, fontWeight: 600, letterSpacing: '-0.02em' }}>This link has expired</h1>
              <p className="lead" style={{ margin: 0, fontSize: 15, color: 'var(--ink-2)', lineHeight: 1.5 }}>
                Links work once, for 24 hours. In the ShopMate app, sign in with your email and tap
                {' '}<b>Send the email again</b> for a new one.
              </p>
            </>
          ) : state === 'confirmed' ? (
            <>
              <h1 style={{ margin: 0, fontSize: 26, fontWeight: 600, letterSpacing: '-0.02em' }}>Your email is confirmed</h1>
              <p className="lead" style={{ margin: 0, fontSize: 15, color: 'var(--ink-2)', lineHeight: 1.5 }}>
                Go back to the ShopMate app and sign in with your email and password to set up your shop.
              </p>
            </>
          ) : null}
        </div>
      </div>
    </div>
  );
}
