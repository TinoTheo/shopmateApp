# ShopMate website

The public site: what ShopMate is, who it's for (premium independent stores in Zimbabwe and South
Africa), and how to book a walkthrough. It's a static Next.js site with no server and no accounts.
The walkthrough form writes the request as a WhatsApp message and opens it, so nothing is sent
until the owner taps send.

Contact details and the dashboard address live in `lib/site.ts`.

## Run it locally

```
npm install
npm run dev
```

Then open http://localhost:3001.

## Put it online (Vercel)

1. Vercel › Add New › Project › import `TinoTheo/shopmateApp`.
2. **Root Directory: `site`.** Framework: Next.js. No environment variables are needed.
3. Deploy. When ShopMate has a domain, add it under Settings › Domains: the website takes the
   main domain (for example `shopmate.africa`), and the dashboard moves to `app.` on the same domain.

## Before launch

- Replace the placeholders: `EMAIL` in `lib/site.ts`, and the striped "Photo to come" blocks on
  the home page (`components/Placeholder.tsx` marks each one).
- Confirm the pricing page's plan contents (support levels, named contact) and the FAQ answers.
- Have a lawyer review `/privacy` and `/terms`, and fill in every `[bracket]`.
- Add receipt printing and barcode scanning to the copy only once they ship.
