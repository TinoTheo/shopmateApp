import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';

// Inter, as in the till app. next/font self-hosts it at build time: no request to Google at runtime.
const inter = Inter({ subsets: ['latin'], weight: ['400', '500', '600'], variable: '--font-inter' });

export const metadata: Metadata = {
  title: 'ShopMate',
  description: "Your shop's sales, tills and stock, across every till.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={inter.variable}>
      <body>{children}</body>
    </html>
  );
}
