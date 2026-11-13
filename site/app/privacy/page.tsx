import type { Metadata } from 'next';
import { EMAIL } from '@/lib/site';

export const metadata: Metadata = { title: 'Privacy policy' };

export default function PrivacyPage() {
  return (
    <>
      <section className="green page-head">
        <div className="wrap">
          <div className="eyebrow light">The fine print</div>
          <h1 style={{ marginTop: 16 }}>Privacy policy</h1>
          <p className="lead">How ShopMate looks after your store’s information and the people who use it.</p>
        </div>
      </section>
      <section className="section">
        <div className="wrap legal">
          <div className="draft">
            Draft for review. This policy must be checked by a lawyer against South Africa’s POPIA and
            Zimbabwe’s Cyber and Data Protection Act before it is relied on. Items in [brackets] need filling in.
          </div>
          <p>Last updated: [date]</p>

          <h2>Who we are</h2>
          <p>
            ShopMate ([registered business name and number]) provides a point-of-sale, stock and reporting service for
            independent stores. In this policy, “we” means ShopMate, and “you” means the store owner who signs up, and
            the people they add to their store.
          </p>

          <h2>What we keep</h2>
          <ul>
            <li><strong>The owner’s account:</strong> name, email address and a password, which we never see in the clear.</li>
            <li><strong>Your store’s records:</strong> products, prices, stock movements, sales and receipts, and your store’s name and settings.</li>
            <li><strong>Your team:</strong> the names and roles you add, and their PINs, stored only as a one-way scrambled code.</li>
            <li><strong>Your devices:</strong> each till’s name, a short code and when it last connected.</li>
            <li><strong>Walkthrough requests:</strong> the details you send us on WhatsApp or by email.</li>
          </ul>
          <p>We don’t ask for, and don’t keep, your customers’ names or contact details. Card numbers never pass through ShopMate.</p>

          <h2>Why we keep it</h2>
          <ul>
            <li>To run the service: record your sales, keep your stock and show you your reports.</li>
            <li>To keep it secure: sign you in, and make sure only your store’s people can see your store’s records.</li>
            <li>To support you, and to tell you about your plan and important changes.</li>
          </ul>

          <h2>Who owns it</h2>
          <p>
            Your store’s records belong to your store. We never sell them, and we never share one store’s records with
            another business without that store’s clear, written permission.
          </p>

          <h2>Where it’s kept</h2>
          <p>
            Records are kept on each till and on our hosting provider, Supabase, in [region]. Access is limited to the
            people of your store, and to ShopMate staff only when needed to support you.
          </p>

          <h2>How long we keep it</h2>
          <p>[For as long as your store uses ShopMate, and for [period] afterwards so you can retrieve your records, unless the law requires longer.]</p>

          <h2>Your rights</h2>
          <p>
            You can ask to see the information we hold about you, to correct it, or to delete it, and you can object to
            how we use it. To do any of this, email <a href={`mailto:${EMAIL}`}>{EMAIL}</a>. You can also complain to the
            Information Regulator (South Africa) or [the relevant authority in Zimbabwe].
          </p>

          <h2>Changes</h2>
          <p>If we change this policy, we’ll update the date above and tell store owners about anything important.</p>
        </div>
      </section>
    </>
  );
}
