import type { Metadata } from 'next';
import Link from 'next/link';

export const metadata: Metadata = { title: 'Pricing' };

const PLANS = [
  {
    name: 'Single store',
    for: 'One store, one or two tills.',
    items: ['The till on your phones or tablets', 'Stock with full history', 'Team PINs and roles', 'Owner dashboard'],
  },
  {
    name: 'Growing',
    for: 'Two or three branches.',
    items: ['Everything in Single store', 'Every branch in one dashboard', 'Tills in each branch', 'Priority support on WhatsApp'],
    featured: true,
  },
  {
    name: 'Group',
    for: 'Four branches or more.',
    items: ['Everything in Growing', 'As many branches and tills as you need', 'Help setting up each branch', 'A named contact at ShopMate'],
  },
];

const FAQ = [
  { q: 'Is there a trial?', a: 'Yes. After your walkthrough you can use ShopMate in your store for 30 days before deciding.' },
  { q: 'What do I need to run it?', a: 'An Android phone or tablet for each till, and an internet connection some of the time. ShopMate keeps selling offline and sends everything when it’s back.' },
  { q: 'Can I pay in USD or Rand?', a: 'Yes. We’ll agree the currency and payment method that suits your store.' },
  { q: 'Who owns my data?', a: 'You do. Your sales and stock belong to your store. Read how we look after them in our privacy policy.' },
  { q: 'What if my plan ends?', a: 'You get a week’s notice in the app and a week of grace after it ends. Sales you’ve already taken are always kept and sent.' },
];

export default function PricingPage() {
  return (
    <>
      <section className="green page-head">
        <div className="wrap">
          <div className="eyebrow light">Pricing</div>
          <h1 style={{ marginTop: 16 }}>Plans for one store, or several.</h1>
          <p className="lead">Every store runs differently, so we agree your plan in your walkthrough. Here’s what each one includes.</p>
        </div>
      </section>

      <section className="section">
        <div className="wrap">
          <div className="plans">
            {PLANS.map((p) => (
              <div key={p.name} className={`plan${p.featured ? ' featured' : ''}`}>
                {p.featured ? <div className="eyebrow" style={{ color: 'var(--green)' }}>Most chosen</div> : null}
                <h3>{p.name}</h3>
                <p className="for">{p.for}</p>
                <ul className="ticks">{p.items.map((i) => <li key={i}>{i}</li>)}</ul>
                <p className="price">Price agreed in your walkthrough</p>
                <Link href="/walkthrough/" className={`btn ${p.featured ? 'btn-green' : 'btn-line'}`}>Book a walkthrough</Link>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="section white tight">
        <div className="wrap">
          <div className="section-head"><div className="eyebrow">Questions</div><h2>Good to know.</h2></div>
          <div className="faq">
            {FAQ.map((f) => (
              <details key={f.q}>
                <summary>{f.q}</summary>
                <p>{f.a}</p>
              </details>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
