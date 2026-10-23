-- Tenancy, staff and licensing (CLAUDE.md § Tenancy, staff and licensing).
--
--   licences          one per organisation: plan, expiry, store and staff limits
--   current_licence   what the till reads at sign-in, store pick and every sync
--   create_store      owners and managers add a store, within the plan's store limit
--   invites           6-character codes; create_invite / redeem_invite
--   list_staff        the staff list in Settings (emails come from auth.users)
--   sales policy      a sale is accepted while the licence, plus 7 days' grace, covers the
--                     time it was taken — a sale taken in time always uploads, however late
--
-- Licences are set by ShopMate from the dashboard; members can only read theirs. To extend one:
--
--   update public.licences set plan = 'basic', expires_at = now() + interval '30 days',
--     max_stores = 1, max_staff = 5, updated_at = now()
--   where organization_id = '<org id>';
--
-- Safe to re-run.

begin;

-- Licences -----------------------------------------------------------------------------------

do $$ begin
  create type public.licence_plan as enum ('trial', 'basic', 'pro');
exception when duplicate_object then null; end $$;

create table if not exists public.licences (
  organization_id uuid primary key references public.organizations(id) on delete cascade,
  plan public.licence_plan not null default 'trial',
  expires_at timestamptz not null,
  max_stores integer not null,
  max_staff integer not null,
  updated_at timestamptz not null default now()
);

alter table public.licences enable row level security;

drop policy if exists "members read licence" on public.licences;
create policy "members read licence" on public.licences
  for select
  using (organization_id in (select public.auth_org_ids()));
-- No write policy on purpose: only the dashboard (service role) changes a licence.

-- Every organisation that exists today starts a 30-day trial now.
insert into public.licences (organization_id, plan, expires_at, max_stores, max_staff)
select id, 'trial', now() + interval '30 days', 1, 3
from public.organizations
on conflict (organization_id) do nothing;

-- Days after expiry during which new sales are still accepted.
create or replace function public.licence_grace()
returns interval
language sql
immutable
as $$ select interval '7 days' $$;

-- True when the store's licence (plus grace) covers [p_at].
create or replace function public.licence_allows_sales(p_store_id uuid, p_at timestamptz)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.licences l
    join public.stores s on s.organization_id = l.organization_id
    where s.id = p_store_id
      and p_at <= l.expires_at + public.licence_grace()
  )
$$;

-- The licence of the org that owns [p_store_id], with how much of it is used. Empty for a
-- store the caller isn't a member of.
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
    (select count(*)::integer from public.memberships m2
       where m2.organization_id = l.organization_id and m2.role <> 'owner')
  from public.licences l
  join public.stores s on s.organization_id = l.organization_id
  where s.id = p_store_id
    and s.organization_id in (select public.auth_org_ids())
$$;

-- Sales: accepted while the licence covers the moment of sale.
drop policy if exists "insert sales" on public.sales;
create policy "insert sales" on public.sales
  for insert
  with check (
    store_id in (select public.auth_accessible_store_ids())
    and public.licence_allows_sales(store_id, created_at)
  );

-- New organisations get their trial in the same transaction.
create or replace function public.create_organization_with_store(org_name text, store_name text, currency text default '$')
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  new_org_id uuid;
  new_store_id uuid;
  new_membership_id uuid;
begin
  insert into organizations (name) values (org_name)
    returning id into new_org_id;

  insert into stores (organization_id, name, currency_symbol)
    values (new_org_id, store_name, currency)
    returning id into new_store_id;

  insert into memberships (user_id, organization_id, role)
    values (auth.uid(), new_org_id, 'owner')
    returning id into new_membership_id;

  insert into store_access (membership_id, store_id)
    values (new_membership_id, new_store_id);

  insert into licences (organization_id, plan, expires_at, max_stores, max_staff)
    values (new_org_id, 'trial', now() + interval '30 days', 1, 3);

  return new_store_id;
end;
$$;

-- Stores -------------------------------------------------------------------------------------

-- An owner or manager adds a store to their organisation. Refused past the plan's store limit
-- ('store_limit') or once the licence and its grace have run out ('licence_expired').
create or replace function public.create_store(p_name text, p_currency text default '$')
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_org uuid;
  v_membership uuid;
  v_store uuid;
  v_licence public.licences%rowtype;
begin
  select m.organization_id, m.id into v_org, v_membership
  from memberships m
  where m.user_id = auth.uid() and m.role in ('owner', 'manager')
  order by m.created_at
  limit 1;
  if v_org is null then
    raise exception 'not_allowed' using errcode = '42501';
  end if;

  select * into v_licence from licences where organization_id = v_org;
  if v_licence is null or now() > v_licence.expires_at + licence_grace() then
    raise exception 'licence_expired' using errcode = 'P0001';
  end if;
  if (select count(*) from stores where organization_id = v_org) >= v_licence.max_stores then
    raise exception 'store_limit' using errcode = 'P0001';
  end if;

  insert into stores (organization_id, name, currency_symbol)
    values (v_org, trim(p_name), p_currency)
    returning id into v_store;
  insert into store_access (membership_id, store_id)
    values (v_membership, v_store);
  return v_store;
end;
$$;

-- A fresh till loads its store's catalogue from the latest snapshot, so members read them.
drop policy if exists "read store snapshots" on public.store_snapshots;
create policy "read store snapshots" on public.store_snapshots
  for select
  using (store_id in (select public.auth_accessible_store_ids()));

-- Invites ------------------------------------------------------------------------------------

create table if not exists public.invites (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  organization_id uuid not null references public.organizations(id) on delete cascade,
  store_id uuid not null references public.stores(id) on delete cascade,
  role public.member_role not null check (role <> 'owner'),
  can_manage_stock boolean not null default false,
  created_by uuid not null,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null default now() + interval '7 days',
  redeemed_by uuid,
  redeemed_at timestamptz
);

alter table public.invites enable row level security;

drop policy if exists "managers read invites" on public.invites;
create policy "managers read invites" on public.invites
  for select
  using (public.auth_has_role(store_id, array['owner', 'manager']::public.member_role[]));

drop policy if exists "managers cancel invites" on public.invites;
create policy "managers cancel invites" on public.invites
  for delete
  using (public.auth_has_role(store_id, array['owner', 'manager']::public.member_role[]));
-- Created and redeemed only through the functions below.

-- An owner invites a manager or cashier; a manager invites cashiers only. Refused past the
-- plan's staff limit, counting open invites ('staff_limit'), or with the licence run out.
create or replace function public.create_invite(
  p_store_id uuid,
  p_role public.member_role,
  p_can_manage_stock boolean default false
)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  v_org uuid;
  v_licence public.licences%rowtype;
  v_used integer;
  v_code text;
  v_alphabet constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';  -- no 0/O, 1/I
begin
  if p_role = 'owner' then
    raise exception 'not_allowed' using errcode = '42501';
  end if;
  if not (
    auth_has_role(p_store_id, array['owner']::member_role[])
    or (p_role = 'cashier' and auth_has_role(p_store_id, array['manager']::member_role[]))
  ) then
    raise exception 'not_allowed' using errcode = '42501';
  end if;

  select organization_id into v_org from stores where id = p_store_id;
  select * into v_licence from licences where organization_id = v_org;
  if v_licence is null or now() > v_licence.expires_at + licence_grace() then
    raise exception 'licence_expired' using errcode = 'P0001';
  end if;

  select
    (select count(*) from memberships where organization_id = v_org and role <> 'owner')
    + (select count(*) from invites
         where organization_id = v_org and redeemed_at is null and expires_at > now())
  into v_used;
  if v_used >= v_licence.max_staff then
    raise exception 'staff_limit' using errcode = 'P0001';
  end if;

  loop
    select string_agg(substr(v_alphabet, 1 + floor(random() * length(v_alphabet))::integer, 1), '')
      into v_code
      from generate_series(1, 6);
    exit when not exists (select 1 from invites where code = v_code);
  end loop;

  insert into invites (code, organization_id, store_id, role, can_manage_stock, created_by)
    values (v_code, v_org, p_store_id, p_role, p_can_manage_stock, auth.uid());
  return v_code;
end;
$$;

-- The signed-in user joins the invite's organisation and store. Returns the store id, which
-- becomes the till's active store. Errors: 'invite_not_found' (wrong, used or expired code).
create or replace function public.redeem_invite(p_code text)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_invite public.invites%rowtype;
  v_membership uuid;
begin
  if auth.uid() is null then
    raise exception 'not_allowed' using errcode = '42501';
  end if;

  select * into v_invite
  from invites
  where code = upper(regexp_replace(p_code, '\s', '', 'g'))
    and redeemed_at is null
    and expires_at > now()
  for update;
  if v_invite.id is null then
    raise exception 'invite_not_found' using errcode = 'P0001';
  end if;

  select id into v_membership
  from memberships
  where user_id = auth.uid() and organization_id = v_invite.organization_id;
  if v_membership is null then
    insert into memberships (user_id, organization_id, role)
      values (auth.uid(), v_invite.organization_id, v_invite.role)
      returning id into v_membership;
  end if;

  if not exists (
    select 1 from store_access where membership_id = v_membership and store_id = v_invite.store_id
  ) then
    insert into store_access (membership_id, store_id, can_manage_stock)
      values (v_membership, v_invite.store_id, v_invite.can_manage_stock);
  end if;

  update invites set redeemed_by = auth.uid(), redeemed_at = now() where id = v_invite.id;
  return v_invite.store_id;
end;
$$;

-- Staff --------------------------------------------------------------------------------------

-- Everyone in the caller's organisation, one row per store they can work in. Owners and
-- managers only. Emails live in auth.users, which members can't read directly.
create or replace function public.list_staff()
returns table (
  membership_id uuid,
  user_id uuid,
  email text,
  role public.member_role,
  store_id uuid,
  store_name text,
  can_manage_stock boolean,
  joined_at timestamptz
)
language sql
stable
security definer
set search_path = public
as $$
  select m.id, m.user_id, u.email::text, m.role, s.id, s.name,
         coalesce(sa.can_manage_stock, false), m.created_at
  from memberships m
  join auth.users u on u.id = m.user_id
  left join store_access sa on sa.membership_id = m.id
  left join stores s on s.id = sa.store_id
  where m.organization_id in (
    select organization_id from memberships
    where user_id = auth.uid() and role in ('owner', 'manager')
  )
  order by m.role, u.email
$$;

revoke all on function public.licence_allows_sales(uuid, timestamptz) from public;
revoke all on function public.current_licence(uuid) from public;
revoke all on function public.create_store(text, text) from public;
revoke all on function public.create_invite(uuid, public.member_role, boolean) from public;
revoke all on function public.redeem_invite(text) from public;
revoke all on function public.list_staff() from public;
grant execute on function public.licence_allows_sales(uuid, timestamptz) to authenticated;
grant execute on function public.current_licence(uuid) to authenticated;
grant execute on function public.create_store(text, text) to authenticated;
grant execute on function public.create_invite(uuid, public.member_role, boolean) to authenticated;
grant execute on function public.redeem_invite(text) to authenticated;
grant execute on function public.list_staff() to authenticated;

commit;
