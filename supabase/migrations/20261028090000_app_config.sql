-- App settings ShopMate can change without a new APK (CLAUDE.md § Remote settings).
--
-- A handful of public, harmless values every app reads — before sign-in too, because the sign-up
-- confirmation link is built before the owner has a session. Never keys, prices, licence limits
-- or anything about one shop: those have their own protected tables.
--
-- Read by anyone; written by nobody through the API. There are deliberately no insert, update or
-- delete policies, so the only way to change a value is the Supabase dashboard (or SQL) under
-- ShopMate's own login.
--
--   dashboard_url     the web dashboard's address. The app accepts it only on a domain it
--                     already trusts (https, ShopMate's own domains); anything else is ignored.
--   support_contact   shown in Settings as "Need help?", e.g. a WhatsApp number.
--   min_version_code  apps older than this show "A newer ShopMate is available". A notice,
--                     never a lock: an old till still sells.
--   notice            one calm line shown in Settings, e.g. planned maintenance. Empty for none.
--
-- To change one:  update public.app_config set value = '…', updated_at = now() where key = '…';
--
-- Safe to run more than once.

begin;

create table if not exists public.app_config (
  key text primary key,
  value text,
  updated_at timestamptz not null default now()
);

alter table public.app_config enable row level security;

drop policy if exists "anyone reads app config" on public.app_config;
create policy "anyone reads app config" on public.app_config
  for select to anon, authenticated using (true);

revoke insert, update, delete on public.app_config from anon, authenticated;

insert into public.app_config (key, value) values
  ('dashboard_url', 'https://shopmate-remote.vercel.app'),
  ('support_contact', null),
  ('min_version_code', null),
  ('notice', null)
on conflict (key) do nothing;

commit;
