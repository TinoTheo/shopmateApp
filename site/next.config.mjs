/** A static site: `out/` can be hosted anywhere (Vercel, Netlify, Cloudflare Pages). */
const nextConfig = {
  output: 'export',
  trailingSlash: true,
  images: { unoptimized: true },
};

export default nextConfig;
