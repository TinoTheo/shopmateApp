import type { Metadata } from 'next';
import { EMAIL } from '@/lib/site';

export const metadata: Metadata = { title: 'Terms of service' };

export default function TermsPage() {
  return (
    <>
      <section className="green page-head">
        <div className="wrap">
          <div className="eyebrow light">The fine print</div>
          <h1 style={{ marginTop: 16 }}>Terms of service</h1>
          <p className="lead">The agreement between ShopMate and the stores that use it.</p>
        </div>
      </section>
      <section className="section">
        <div className="wrap legal">
          <div className="draft">
            Draft for review. These terms must be checked by a lawyer before they are relied on. Items in [brackets] need filling in.
          </div>
          <p>Last updated: [date]</p>

          <h2>The service</h2>
          <p>
            ShopMate provides an app for recording sales and stock on Android devices, and a web dashboard for store
            owners. These terms apply to the store owner who signs up and to everyone they add to their store.
          </p>

          <h2>Your account</h2>
          <p>
            The owner is responsible for their account and for the people they add. Keep your password private, and
            give each team member their own PIN.
          </p>

          <h2>Plans, trials and payment</h2>
          <p>
            Your plan, its price and its limits (stores, staff and tills) are agreed with you. A trial lasts 30 days.
            When a plan ends, the app gives notice and a week of grace; after that, new sales are paused until it is
            renewed. Sales already recorded are always kept and sent. [Payment terms, currency and notice period.]
          </p>

          <h2>Your records</h2>
          <p>
            Your store’s records belong to you. You give us permission to store and process them only to provide the
            service. See our privacy policy for how we look after them.
          </p>

          <h2>Fair use</h2>
          <p>Use ShopMate for your store’s lawful business. Don’t try to reach another store’s records or disrupt the service.</p>

          <h2>Availability</h2>
          <p>
            The app keeps recording sales without an internet connection and sends them when it’s back. We work to keep
            the online service available, but can’t promise it will never be interrupted. [Support hours and response times.]
          </p>

          <h2>Liability</h2>
          <p>[Limits of liability, to be written with a lawyer.]</p>

          <h2>Ending the agreement</h2>
          <p>You can stop using ShopMate at any time. [How to retrieve your records, and for how long.]</p>

          <h2>Contact</h2>
          <p>Questions about these terms: <a href={`mailto:${EMAIL}`}>{EMAIL}</a>.</p>
        </div>
      </section>
    </>
  );
}
