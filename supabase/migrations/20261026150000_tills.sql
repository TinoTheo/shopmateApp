-- Tills (CLAUDE.md § Devices — tills and remote): the store's registered selling devices.
--
--   tills                one row per device the owner chose to sell on, by till code
--   licences.max_tills   how many active tills the plan allows (trial: 2)
--   register_till        the device registers (or re-registers) itself; refused past the limit
--   till_seen            each sync's heartbeat; answers whether the till is still allowed
--
-- A device that isn't a till is a remote-management device and never appears here.
-- Safe to re-run.

begin;

alter table public.licences add column if not exists max_tills integer not null default 2;

create table if not exists public.tills (
  id uuid primary key default gen_random_uuid(),
  store_id uuid not null references public.stores(id) on delete cascade,
  code text not null,
  name text not null,
  registered_by uuid not null,
  created_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now(),
  revoked_at timestamptz,
  unique (store_id, code)
);

alter table public.tills enable row level security;

drop policy if exists "members read tills" on public.tills;
create policy "members read tills" on public.tills
  for select
  using (store_id in (select public.auth_accessible_store_ids()));

-- Removing a till is the owner's: they set revoked_at. Registering goes through register_till.
drop policy if exists "owners revoke tills" on public.tills;
create policy "owners revoke tills" on public.tills
  for update
  using (public.auth_has_role(store_id, array['owner']::public.member_role[]))
  with check (public.auth_has_role(store_id, array['owner']::public.member_role[]));

-- Registers this device as a till of [p_store_id], or brings back its existing row. Refused
-- ('till_limit') when the organisation already has its plan's number of active tills, and
-- ('not_allowed') for someone who isn't a member of the store.
create or replace function public.register_till(p_store_id uuid, p_code text, p_name text)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_org uuid;
  v_till public.tills%rowtype;
  v_max integer;
  v_active integer;
begin
  if p_store_id not in (select auth_accessible_store_ids()) then
    raise exception 'not_allowed' using errcode = '42501';
  end if;

  select * into v_till from tills where store_id = p_store_id and code = p_code;
  if v_till.id is not null and v_till.revoked_at is null then
    update tills set name = trim(p_name), last_seen_at = now() where id = v_till.id;
    return v_till.id;
  end if;

  select organization_id into v_org from stores where id = p_store_id;
  select max_tills into v_max from licences where organization_id = v_org;
  select count(*) into v_active
  from tills t join stores s on s.id = t.store_id
  where s.organization_id = v_org and t.revoked_at is null;
  if v_max is null or v_active >= v_max then
    raise exception 'till_limit' using errcode = 'P0001';
  end if;

  if v_till.id is not null then
    -- A removed till asked to be a till again: the owner chose this on the device itself.
    update tills set revoked_at = null, name = trim(p_name), last_seen_at = now(), registered_by = auth.uid()
    where id = v_till.id;
    return v_till.id;
  end if;

  insert into tills (store_id, code, name, registered_by)
  values (p_store_id, p_code, trim(p_name), auth.uid())
  returning id into v_till.id;
  return v_till.id;
end;
$$;

-- Heartbeat from a till's sync. Returns 'active', 'revoked', or 'unknown' (no such till).
create or replace function public.till_seen(p_store_id uuid, p_code text)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  v_till public.tills%rowtype;
begin
  if p_store_id not in (select auth_accessible_store_ids()) then
    raise exception 'not_allowed' using errcode = '42501';
  end if;
  select * into v_till from tills where store_id = p_store_id and code = p_code;
  if v_till.id is null then
    return 'unknown';
  end if;
  if v_till.revoked_at is not null then
    return 'revoked';
  end if;
  update tills set last_seen_at = now() where id = v_till.id;
  return 'active';
end;
$$;

-- current_licence also reports tills, so Settings can show "1 of 2 tills".
drop function if exists public.current_licence(uuid);
create function public.current_licence(p_store_id uuid)
returns table (
  plan public.licence_plan,
  expires_at timestamptz,
  grace_ends_at timestamptz,
  max_stores integer,
  max_staff integer,
  stores_used integer,
  staff_used integer,
  max_tills integer,
  tills_used integer
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
       where s3.organization_id = l.organization_id and st.active and st.role <> 'owner'),
    l.max_tills,
    (select count(*)::integer
       from public.tills t join public.stores s4 on s4.id = t.store_id
       where s4.organization_id = l.organization_id and t.revoked_at is null)
  from public.licences l
  join public.stores s on s.organization_id = l.organization_id
  where s.id = p_store_id
    and s.organization_id in (select public.auth_org_ids())
$$;

revoke all on function public.register_till(uuid, text, text) from public;
revoke all on function public.till_seen(uuid, text) from public;
revoke all on function public.current_licence(uuid) from public;
grant execute on function public.register_till(uuid, text, text) to authenticated;
grant execute on function public.till_seen(uuid, text) to authenticated;
grant execute on function public.current_licence(uuid) to authenticated;

commit;
