'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { Lockup } from '@/components/Lockup';
import { notConfigured, supabase } from '@/lib/supabase';

type Msg = { text: string; error?: boolean } | null;

/** Sign in (handoff 1b phone, 1c desktop). The same email and password as the ShopMate app. */
export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [msg, setMsg] = useState<Msg>(null);
  const [busy, setBusy] = useState(false);
  const [wide, setWide] = useState(true);

  useEffect(() => {
    const m = window.matchMedia('(min-width: 1024px)');
    const set = () => setWide(m.matches);
    set();
    m.addEventListener('change', set);
    return () => m.removeEventListener('change', set);
  }, []);

  const offline = () => typeof navigator !== 'undefined' && !navigator.onLine;

  async function signIn(e: React.FormEvent) {
    e.preventDefault();
    if (offline()) return setMsg({ text: 'You’re offline. Connect to sign in.' });
    setBusy(true);
    setMsg(null);
    const { error } = await supabase.auth.signInWithPassword({ email: email.trim(), password });
    setBusy(false);
    if (!error) return router.replace('/');
    if (error.status === 400 || /invalid/i.test(error.message)) {
      setMsg({ text: 'That email and password don’t match. Try again or send a reset link.' });
    } else if (offline() || error.status === 0 || /fetch/i.test(error.message)) {
      setMsg({ text: 'You’re offline. Connect to sign in.' });
    } else {
      setMsg({ text: 'ShopMate didn’t answer. Try again in a minute.', error: true });
    }
  }

  async function sendReset() {
    if (!email.trim()) return setMsg({ text: 'Type your email above, then send the link.' });
    if (offline()) return setMsg({ text: 'You’re offline. Connect to send a reset link.' });
    const { error } = await supabase.auth.resetPasswordForEmail(email.trim(), {
      redirectTo: `${window.location.origin}/reset/`,
    });
    setMsg(error
      ? { text: 'ShopMate didn’t answer. Try again in a minute.', error: true }
      : { text: `If ${email.trim()} has a ShopMate account, a link to choose a new password is on its way.` });
  }

  const reset = (
    <button type="button" className="link" style={{ fontSize: wide ? 13 : 14 }} onClick={sendReset}>Send me a reset link</button>
  );

  return (
    <div className="signin">
      <div className="brand">
        <div className="stage">
          <Lockup width={wide ? 360 : 220} delay={400} />
        </div>
        <div className="promise">No data drops. No Friday surprises.</div>
      </div>
      <div className="form-sheet">
        <form onSubmit={signIn} noValidate>
          <h1>Sign in to your shops</h1>
          <p className="lead">See sales, stock and tills from anywhere.</p>
          {notConfigured && <p className="msg err">This dashboard was built without its Supabase settings.</p>}
          <label className="field">
            <span className="lr">Email</span>
            <input type="email" autoComplete="email" placeholder="you@yourshop.co.zw" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label className="field">
            <span className="lr">Password {wide ? reset : null}</span>
            <input type="password" autoComplete="current-password" placeholder="Your password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          {msg && <p className={`msg${msg.error ? ' err' : ''}`} role="status">{msg.text}</p>}
          <button className="btn" type="submit" disabled={busy || notConfigured}>{busy ? 'Signing in…' : 'Sign in'}</button>
          {!wide && <p className="msg" style={{ textAlign: 'center' }}>Forgot it? {reset}</p>}
          {wide && <hr className="hair" />}
          <p className="note">Cashiers open their till in the ShopMate app, not here.</p>
        </form>
      </div>
    </div>
  );
}
