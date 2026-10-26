-- Staff with PINs (CLAUDE.md § Staff — email for the owner, PINs for everyone else).
--
-- Only the owner has an email account. Managers and cashiers are rows here: a name, a role, a
-- PIN hash and salt (PBKDF2-SHA256, hashed on the till that set it), and for a cashier the stock
-- grant. Every till of the store pulls the rows on sync so staff can unlock it offline.
--
-- Removing someone sets active = false: their name stays on the sales they rang up.
-- The invite-code functions from 20261023090000 are no longer used by the app; they are left in
-- place and harmless.
--
-- Safe to re-run.

begin;

create table if not exists public.staff (
  id uuid primary key default gen_random_uuid(),
  store_id uuid not null references public.stores(id) on delete cascade,
  name text not null check (length(trim(name)) > 0),
  role public.member_role not null,
  pin_hash text not null,
  pin_salt text not null,
  can_manage_stock boolean not null default false,
  -- Set on the owner's own row (their till PIN); null for staff.
  user_id uuid,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Two people with the same name on one till would be indistinguishable on "Who's selling?".
create unique index if not exists staff_store_name_active
  on public.staff (store_id, lower(trim(name)))
  where active;

alter table public.staff enable row level security;

drop policy if exists "members read staff" on public.staff;
create policy "members read staff" on public.staff
  for select
  using (store_id in (select public.auth_accessible_store_ids()));

drop policy if exists "owners manage staff" on public.staff;
create policy "owners manage staff" on public.staff
  for all
  using (public.auth_has_role(store_id, array['owner']::public.member_role[]))
  with check (public.auth_has_role(store_id, array['owner']::public.member_role[]));

-- The plan's staff limit counts active staff across the organisation, not the owner.
create or replace function public.staff_within_limit()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_org uuid;
  v_max integer;
  v_used integer;
begin
  if not new.active or new.role = 'owner' then
    return new;
  end if;
  select organization_id into v_org from stores where id = new.store_id;
  select max_staff into v_max from licences where organization_id = v_org;
  select count(*) into v_used
  from staff st join stores s on s.id = st.store_id
  where s.organization_id = v_org and st.active and st.role <> 'owner' and st.id <> new.id;
  if v_max is null or v_used >= v_max then
    raise exception 'staff_limit' using errcode = 'P0001';
  end if;
  new.updated_at := now();
  return new;
end;
$$;

drop trigger if exists staff_within_limit on public.staff;
create trigger staff_within_limit
  before insert or update of active, role on public.staff
  for each row execute function public.staff_within_limit();

-- Who rang up each sale. cashier_name already carries the name.
alter table public.sales add column if not exists staff_id uuid;

-- current_licence counts PIN staff now that invites are gone.
create or replace function public.current_licence(p_store_id uuid)
returns table (
  plan public.licence_plan,
  expires_at timestamptz,
  grace_ends_at timestamptz,
  max_stores integer,
  max_staff integer,
  stores_used integer,
  staff_used integer
)
language sql
stable
security definer
set search_path = public
as $$
  select
    l.plan,
    l.expires_at,
    l.expires_at + public.licence_grace(),
    l.max_stores,
    l.max_staff,
    (select count(*)::integer from public.stores s2 where s2.organization_id = l.organization_id),
    (select count(*)::integer
       from public.staff st join public.stores s3 on s3.id = st.store_id
       where s3.organization_id = l.organization_id and st.active and st.role <> 'owner')
  from public.licences l
  join public.stores s on s.organization_id = l.organization_id
  where s.id = p_store_id
    and s.organization_id in (select public.auth_org_ids())
$$;

commit;
