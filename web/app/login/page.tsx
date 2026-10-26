'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { notConfigured, supabase } from '@/lib/supabase';

/** The owner's sign-in — the same email and password as the till app. */
export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function signIn(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const { error } = await supabase.auth.signInWithPassword({ email: email.trim(), password });
    setBusy(false);
    if (error) {
      setError(
        error.message.toLowerCase().includes('invalid')
          ? "That email and password don't match. Use the ones you sign in to the ShopMate app with."
          : "Can't reach ShopMate. Check the connection and try again.",
      );
      return;
    }
    router.replace('/');
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <div style={{ height: 140, padding: '32px' }}>
        {/* The brand owner's lockup, tagline included — logo art, never set as text. */}
        <img src="/lockup.svg" alt="ShopMate — uncompromising control" style={{ height: 64 }} />
      </div>
      <div className="sheet" style={{ margin: 0, display: 'flex', justifyContent: 'center' }}>
        <form onSubmit={signIn} style={{ width: '100%', maxWidth: 380, marginTop: 24 }}>
          <h1 style={{ fontSize: 24, fontWeight: 500, margin: '0 0 6px' }}>Check on your shop</h1>
          <p className="note" style={{ marginTop: 0, marginBottom: 20 }}>
            Sign in with the owner&apos;s email and password from the ShopMate app.
          </p>
          {notConfigured && <p className="note">This dashboard was built without its Supabase settings.</p>}
          <input className="field" type="email" autoComplete="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <input className="field" type="password" autoComplete="current-password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          {error && <p className="note" style={{ color: '#000' }}>{error}</p>}
          <button className="button" type="submit" disabled={busy || notConfigured} style={{ width: '100%' }}>
            {busy ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  );
}
