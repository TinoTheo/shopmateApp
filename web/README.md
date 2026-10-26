# ShopMate dashboard

The owner's web dashboard (CLAUDE.md § Devices — tills and remote): the store's sales, stock and
tills across every till, from any browser. A remote device in the app opens it; the tills never
depend on it.

It is a static site. Every page reads Supabase directly from the browser with the **anon key and
the owner's own session**, so RLS decides what is visible — exactly as in the app. No server,
no service-role key.

## Run it locally

1. Copy `.env.example` to `.env.local` and fill in the same `SUPABASE_URL` and
   `SUPABASE_ANON_KEY` as the app's `local.properties`.
2. `npm install`
3. `npm run dev`, then open http://localhost:3000 and sign in with the owner's email and password.

## Host it

`npm run build` writes the site to `out/`. Any static host works — Vercel, Netlify or Cloudflare
Pages (free tiers are enough). Set `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY`
in the host's environment settings.

Then put its address in the app's `local.properties` as `WEB_DASHBOARD_URL=https://…` and rebuild
the app: a remote device's **Open full reports** goes to `/reports`.

Add the dashboard's address to Supabase › Authentication › URL Configuration so sign-in works
from it.

## Pages

- **Today** — sales so far across all tills; each till's last contact, flagged when it has been
  quiet for over an hour (its sales may not be in yet).
- **Reports** — today, this week, this month, last month: totals, by hour or day, by till,
  payments, best sellers. `?period=week` opens a period directly.
- **Stock** — every product's stock as the sum of all tills' movements, and "why it says N".
- **Tills** — the store's tills; remove one and it stops at its next sync.
