import Link from 'next/link';
import { DASHBOARD_URL, EMAIL, WHATSAPP_DISPLAY, whatsapp } from '@/lib/site';

export function Footer() {
  return (
    <footer className="footer">
      <div className="wrap footer-grid">
        <div>
          <img src="/lockup.svg" alt="ShopMate" className="footer-logo" />
          <p className="footer-promise">No data drops. No Friday surprises.</p>
        </div>
        <div>
          <div className="eyebrow light">Talk to us</div>
          <a href={whatsapp()} target="_blank" rel="noopener">WhatsApp {WHATSAPP_DISPLAY}</a>
          <a href={`mailto:${EMAIL}`}>{EMAIL}</a>
        </div>
        <div>
          <div className="eyebrow light">ShopMate</div>
          <Link href="/walkthrough/">Book a walkthrough</Link>
          <Link href="/pricing/">Pricing</Link>
          <a href={DASHBOARD_URL}>Sign in</a>
        </div>
        <div>
          <div className="eyebrow light">The fine print</div>
          <Link href="/privacy/">Privacy policy</Link>
          <Link href="/terms/">Terms of service</Link>
        </div>
      </div>
      <div className="wrap footer-base">
        <span>© {new Date().getFullYear()} ShopMate</span>
        <span>Made in Zimbabwe 🇿🇼</span>
      </div>
    </footer>
  );
}
