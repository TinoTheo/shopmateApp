'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { Lockup } from '@/components/Lockup';
import { supabase } from '@/lib/supabase';

/**
 * Where the reset email's link lands. Supabase signs the owner in from the link; they choose a
 * new password here and go on to Today. Not mocked in the handoff — words follow its voice rules.
 */
export default function ResetPage() {
  const router = useRouter();
  const [ready, setReady] = useState<boolean | null>(null);
  const [password, setPassword] = useState('');
  const [msg, setMsg] = useState<{ text: string; error?: boolean } | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const { data } = supabase.auth.onAuthStateChange((event, session) => {
      if (event === 'PASSWORD_RECOVERY' || session) setReady(true);
    });
    supabase.auth.getSession().then(({ data: s }) => setReady((r) => r || !!s.session));
    const t = setTimeout(() => setReady((r) => r ?? false), 3000);
    return () => {
      data.subscription.unsubscribe();
      clearTimeout(t);
    };
  }, []);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (password.length < 8) return setMsg({ text: 'Use at least 8 characters.' });
    setBusy(true);
    const { error } = await supabase.auth.updateUser({ password });
    setBusy(false);
    if (error) return setMsg({ text: 'ShopMate didn’t answer. Try again in a minute.', error: true });
    router.replace('/');
  }

  return (
    <div className="signin">
      <div className="brand">
        <div className="stage"><Lockup width={220} delay={0} /></div>
      </div>
      <div className="form-sheet">
        <form onSubmit={save}>
          <h1>Choose a new password</h1>
          {ready === false ? (
            <>
              <p className="lead">This link has expired or was already used. Send yourself a new one from the sign-in page.</p>
              <button type="button" className="btn" onClick={() => router.replace('/login/')}>Go to sign in</button>
            </>
          ) : (
            <>
              <p className="lead">You’ll use it here and in the ShopMate app.</p>
              <label className="field">
                <span className="lr">New password</span>
                <input type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
              </label>
              {msg && <p className={`msg${msg.error ? ' err' : ''}`}>{msg.text}</p>}
              <button className="btn" type="submit" disabled={busy || !ready}>{busy ? 'Saving…' : 'Save new password'}</button>
            </>
          )}
        </form>
      </div>
    </div>
  );
}
