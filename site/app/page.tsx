import Link from 'next/link';
import { Lockup } from '@/components/Lockup';
import { Placeholder } from '@/components/Placeholder';
import { whatsapp } from '@/lib/site';

const STORES = [
  'Boutiques', 'Delis and specialty grocers', 'Wine and spirits', 'Pharmacies', 'Homeware and décor',
  'Concept and gift stores', 'Butcheries and bakeries', 'Stores with two or three branches',
];

/** One product's history, the way ShopMate shows it: why the number on the shelf is what it is. */
const LEDGER = [
  { k: 'Count', w: 'Rudo · Monday 07:50', c: '24', t: 24 },
  { k: 'Sold', w: 'Front counter · Monday', c: '−11', t: 13 },
  { k: 'Delivery', w: 'Tendai · Tuesday 08:10', c: '+12', t: 25 },
  { k: 'Sold', w: 'Both tills · Tuesday', c: '−19', t: 6 },
  { k: 'Correction: damaged', w: 'Rudo · Wednesday 17:40', c: '−1', t: 5 },
  { k: 'Sold', w: 'Front counter · Today to 15:27', c: '−2', t: 3 },
];

export default function Home() {
  return (
    <>
      <section className="green hero">
        <div className="wrap hero-grid">
          <div>
            <div className="eyebrow light">For independent stores</div>
            <h1 style={{ marginTop: 18 }}>
              Uncompromising control, from the counter <em>to wherever you are.</em>
            </h1>
            <p className="lead">
              ShopMate is the till, stock book and daily report for premium independent stores. It keeps
              selling when the connection drops, and shows you every branch from your phone.
            </p>
            <div className="hero-ctas">
              <Link href="/walkthrough/" className="btn btn-neon">Book a private walkthrough</Link>
              <a href="#how" className="btn btn-ghost">See how it works</a>
            </div>
            <p className="hero-note">For stores in Zimbabwe and South Africa. Runs on Android phones and tablets.</p>
          </div>
          <div className="hero-art">
            <Lockup width={380} delay={500} />
          </div>
        </div>
      </section>

      <section className="section">
        <div className="wrap">
          <div className="section-head">
            <div className="eyebrow">Why stores choose ShopMate</div>
            <h2>You built the store. You shouldn’t have to stand at the till to know how it’s doing.</h2>
          </div>
          <div className="cards">
            <div className="card">
              <span className="num">01</span>
              <h3>Stock you can account for</h3>
              <p>Every delivery, sale and count is recorded, with who did it and when. When a number looks wrong, you see why instead of guessing.</p>
            </div>
            <div className="card">
              <span className="num">02</span>
              <h3>Takings you see as they happen</h3>
              <p>Today’s sales for every till and every branch, on your phone or laptop. You don’t wait for the evening cash-up to find out.</p>
            </div>
            <div className="card">
              <span className="num">03</span>
              <h3>A team you can trust, and show it</h3>
              <p>Each person signs in with their own PIN, so every sale carries a name. Clarity for you, and credit for the people who do the work well.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="section white" id="how">
        <div className="wrap">
          <div className="section-head">
            <div className="eyebrow">How it works</div>
            <h2>Up and running in an afternoon.</h2>
            <p className="lead">No servers, no special hardware. ShopMate runs on the Android phones and tablets you already have.</p>
          </div>
          <div className="cards">
            <div className="card">
              <span className="num">Step 1</span>
              <h3>Add your products</h3>
              <p>Names, prices and how many you have. We’ll set this up with you during your walkthrough.</p>
            </div>
            <div className="card">
              <span className="num">Step 2</span>
              <h3>Sell at the counter</h3>
              <p>Tap the items, take cash, card or mobile money, and the till is ready for the next customer. Offline too.</p>
            </div>
            <div className="card">
              <span className="num">Step 3</span>
              <h3>Run it from anywhere</h3>
              <p>Sales, stock and every till come together in your dashboard, wherever you are.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="section" id="stores">
        <div className="wrap">
          <div className="section-head">
            <div className="eyebrow">Who it’s for</div>
            <h2>Made for stores where every item matters.</h2>
            <p className="lead">
              When a single bottle, dress or prescription is worth real money, a rough idea isn’t good enough.
              ShopMate is built for independent stores that run on standards.
            </p>
          </div>
          <div className="stores">{STORES.map((s) => <span key={s}>{s}</span>)}</div>
        </div>
      </section>

      <section className="section white">
        <div className="wrap">
          <div className="feature">
            <div className="feature-text">
              <div className="eyebrow">Every item accounted for</div>
              <h3>Ask any product why it says 3.</h3>
              <p>
                Stock in ShopMate is never typed over. It’s the sum of every delivery, sale, count and correction,
                each with a name and a time. Nothing changes quietly, so the story always adds up.
              </p>
              <ul className="ticks">
                <li>Counts that show who was short, when, and by how much</li>
                <li>Warnings before a best seller runs out</li>
                <li>Every till’s sales in one shared record</li>
              </ul>
            </div>
            <div className="ledger" aria-label="Example product history">
              <div className="ledger-head">
                <strong>Aged gouda, 250g</strong>
                <span className="eyebrow">Why it says 3</span>
              </div>
              {LEDGER.map((r, i) => (
                <div key={i} className="ledger-row">
                  <span className="k">{r.k}</span>
                  <span className="w">{r.w}</span>
                  <span className="c">{r.c}</span>
                  <span className="t">{r.t}</span>
                </div>
              ))}
              <div className="ledger-total"><span>Left now</span><span>3</span></div>
            </div>
          </div>

          <div className="feature flip">
            <div className="feature-text">
              <div className="eyebrow">Your branches, one view</div>
              <h3>Know how the day is going, wherever you are.</h3>
              <p>
                See today’s takings for each till and branch, how they compare with last week, and what runs
                out next. If a till hasn’t checked in, ShopMate says so, instead of showing you half a number.
              </p>
            </div>
            <Placeholder label="the owner dashboard on a laptop and phone" ratio="16 / 11" />
          </div>

          <div className="feature">
            <div className="feature-text">
              <div className="eyebrow">Calm through outages</div>
              <h3>The connection drops. The counter doesn’t.</h3>
              <p>
                Every sale is saved on the device the moment it happens, and sends itself when the connection
                returns. Your customers never wait on the network, and nothing goes missing in between.
              </p>
            </div>
            <Placeholder label="a tablet at the counter, mid-sale, in a softly lit store" ratio="16 / 11" />
          </div>

          <div className="feature flip">
            <div className="feature-text">
              <div className="eyebrow">Your team, accountable and respected</div>
              <h3>Every sale carries a name.</h3>
              <p>
                Each team member unlocks the till with their own PIN. You decide who may receive stock or change
                prices, and who sells. It’s fair to everyone, and it’s clear to you.
              </p>
            </div>
            <Placeholder label="a team member serving a customer at the till" ratio="16 / 11" />
          </div>
        </div>
      </section>

      <section className="section green">
        <div className="wrap about">
          <blockquote>
            “ShopMate brings the control of a modern retail system to independent stores.”
          </blockquote>
          <div style={{ display: 'grid', gap: 16 }}>
            <div className="eyebrow light">Why we built it</div>
            <p>
              Independent stores compete with chains on experience, and deserve the same control chains have,
              without their cost or complexity. ShopMate keeps your counter selling when the connection drops,
              and lets you run the business from wherever you are.
            </p>
            <p>Built in Zimbabwe, for stores across Zimbabwe and South Africa.</p>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="wrap closing">
          <div className="eyebrow">A private walkthrough</div>
          <h2>See ShopMate with your own products, in your own store.</h2>
          <p className="lead">We’ll show you around, set up a few of your products, and answer every question. No obligation.</p>
          <div className="hero-ctas">
            <Link href="/walkthrough/" className="btn btn-green">Book a walkthrough</Link>
            <a href={whatsapp()} target="_blank" rel="noopener" className="btn btn-line">Message us on WhatsApp</a>
          </div>
        </div>
      </section>
    </>
  );
}
