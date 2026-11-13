// Everything the site says about how to reach ShopMate, in one place.

/** International format without the plus, as wa.me needs it. */
export const WHATSAPP_NUMBER = '263773146122';
export const WHATSAPP_DISPLAY = '+263 77 314 6122';

/** Placeholder until ShopMate has its own domain and mailbox. */
export const EMAIL = 'hello@shopmate.example';

/** The owner dashboard, for clients who already use ShopMate. */
export const DASHBOARD_URL = 'https://shopmate-remote.vercel.app';

/** A WhatsApp chat with ShopMate, opened with [text] ready to send. */
export function whatsapp(text = 'Hello ShopMate, I’d like to book a walkthrough.'): string {
  return `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(text)}`;
}
