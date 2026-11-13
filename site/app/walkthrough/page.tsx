'use client';

import { useState } from 'react';
import { EMAIL, WHATSAPP_DISPLAY, whatsapp } from '@/lib/site';

/**
 * Book a walkthrough. A static site has no server to send a form to, so the form writes the
 * request as a WhatsApp message and opens it: nothing is sent until the owner taps send.
 */
export default function WalkthroughPage() {
  const [name, setName] = useState('');
  const [store, setStore] = useState('');
  const [city, setCity] = useState('');
  const [kind, setKind] = useState('');
  const [branches, setBranches] = useState('1');

  function send(e: React.FormEvent) {
    e.preventDefault();
    const lines = [
      'Hello ShopMate, I’d like to book a walkthrough.',
      `Name: ${name.trim()}`,
      `Store: ${store.trim()}${kind ? ` (${kind})` : ''}`,
      city.trim() ? `City: ${city.trim()}` : null,
      `Branches: ${branches}`,
    ].filter(Boolean);
    window.open(whatsapp(lines.join('\n')), '_blank', 'noopener');
  }

  return (
    <>
      <section className="green page-head">
        <div className="wrap">
          <div className="eyebrow light">A private walkthrough</div>
          <h1 style={{ marginTop: 16 }}>See ShopMate with your own products.</h1>
          <p className="lead">
            Thirty to forty-five minutes, at your store or on a video call. We’ll set up a few of your products,
            show you the counter and the dashboard, and answer every question.
          </p>
        </div>
      </section>

      <section className="section">
        <div className="wrap two-col">
          <div style={{ display: 'grid', gap: 32 }}>
            <div className="section-head" style={{ marginBottom: 0 }}>
              <div className="eyebrow">What to expect</div>
              <h2 style={{ fontSize: 34 }}>An unhurried look at how ShopMate would run your store.</h2>
            </div>
            <ol className="steps">
              <li><div><strong>Tell us about your store</strong><p>Send the short form, or message us directly. We reply the same day.</p></div></li>
              <li><div><strong>We walk you through it</strong><p>The counter, stock, your team’s PINs and the owner dashboard, using your own products.</p></div></li>
              <li><div><strong>Try it in your store</strong><p>If it suits you, we set it up on your devices and you try it for 30 days before deciding.</p></div></li>
            </ol>
            <p style={{ color: 'var(--ink-2)' }}>
              Prefer to talk straight away? WhatsApp{' '}
              <a href={whatsapp()} target="_blank" rel="noopener" style={{ color: 'var(--green)', fontWeight: 500 }}>{WHATSAPP_DISPLAY}</a>
              {' '}or email{' '}
              <a href={`mailto:${EMAIL}`} style={{ color: 'var(--green)', fontWeight: 500 }}>{EMAIL}</a>.
            </p>
          </div>

          <form className="form" onSubmit={send}>
            <label className="field">
              <span>Your name</span>
              <input value={name} onChange={(e) => setName(e.target.value)} required autoComplete="name" />
            </label>
            <label className="field">
              <span>Store name</span>
              <input value={store} onChange={(e) => setStore(e.target.value)} required autoComplete="organization" />
            </label>
            <label className="field">
              <span>What kind of store?</span>
              <select value={kind} onChange={(e) => setKind(e.target.value)}>
                <option value="">Choose one</option>
                <option>Boutique</option>
                <option>Deli or specialty grocer</option>
                <option>Wine and spirits</option>
                <option>Pharmacy</option>
                <option>Homeware and décor</option>
                <option>Concept or gift store</option>
                <option>Butchery or bakery</option>
                <option>Something else</option>
              </select>
            </label>
            <label className="field">
              <span>City</span>
              <input value={city} onChange={(e) => setCity(e.target.value)} autoComplete="address-level2" placeholder="Harare, Bulawayo, Johannesburg…" />
            </label>
            <label className="field">
              <span>How many branches?</span>
              <select value={branches} onChange={(e) => setBranches(e.target.value)}>
                <option>1</option>
                <option>2</option>
                <option>3</option>
                <option>4 or more</option>
              </select>
            </label>
            <button type="submit" className="btn btn-green">Send on WhatsApp</button>
            <p className="form-note">This opens WhatsApp with your request written out. Nothing is sent until you tap send.</p>
          </form>
        </div>
      </section>
    </>
  );
}
