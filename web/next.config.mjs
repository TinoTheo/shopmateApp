/**
 * A static site: every page is client-side and reads Supabase directly, through the same RLS as
 * the till app. `out/` can be hosted anywhere (Vercel, Netlify, Cloudflare Pages).
 */
const nextConfig = {
  output: 'export',
  trailingSlash: true,
  images: { unoptimized: true },
};

export default nextConfig;
