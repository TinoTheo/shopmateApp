import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import { Footer } from '@/components/Footer';
import { Header } from '@/components/Header';
import './globals.css';

// Inter, as in the app and the dashboard. next/font hosts it with the site: no runtime request to Google.
const inter = Inter({ subsets: ['latin'], weight: ['400', '500', '600'], variable: '--font-inter' });

export const metadata: Metadata = {
  title: { default: 'ShopMate · Uncompromising control for independent stores', template: '%s · ShopMate' },
  description:
    'The till, stock book and daily report for premium independent stores in Zimbabwe and South Africa. It keeps selling offline, and you run the business from anywhere.',
  icons: { icon: '/mark.svg' },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={inter.variable}>
      <body>
        <Header />
        <main>{children}</main>
        <Footer />
      </body>
    </html>
  );
}
