-- Fix: "infinite recursion detected in policy for relation memberships" (42P17).
--
-- Four policies read `memberships` inline. Reading `memberships` applies its own policies,
-- which read `memberships` again — Postgres stops that with 42P17, and every policy that
-- touches memberships fails with it (organizations, store_access, and anything a signed-in
-- user does that resolves their stores).
--
-- The fix is the same pattern auth_accessible_store_ids() / auth_has_role() already use:
-- SECURITY DEFINER helpers read `memberships` as the function owner, so RLS is not
-- re-applied inside the policy. They only ever return rows for auth.uid(), so they leak
-- nothing a policy wouldn't already allow.
--
-- Safe to re-run: functions use CREATE OR REPLACE, policies are dropped before re-creation.

begin;

-- Organisations the signed-in user belongs to, any role.
create or replace function public.auth_org_ids()
returns setof uuid
language sql
stable
security definer
set search_path = public
as $$
  select organization_id from public.memberships where user_id = auth.uid()
$$;

-- Organisations the signed-in user owns.
create or replace function public.auth_owner_org_ids()
returns setof uuid
language sql
stable
security definer
set search_path = public
as $$
  select organization_id from public.memberships
  where user_id = auth.uid() and role = 'owner'::public.member_role
$$;

revoke all on function public.auth_org_ids() from public;
revoke all on function public.auth_owner_org_ids() from public;
grant execute on function public.auth_org_ids() to authenticated;
grant execute on function public.auth_owner_org_ids() to authenticated;

-- memberships ---------------------------------------------------------------------------

drop policy if exists "read org memberships" on public.memberships;
create policy "read org memberships" on public.memberships
  for select
  using (organization_id in (select public.auth_org_ids()));

drop policy if exists "owners manage memberships" on public.memberships;
create policy "owners manage memberships" on public.memberships
  for all
  using (organization_id in (select public.auth_owner_org_ids()))
  with check (organization_id in (select public.auth_owner_org_ids()));

-- organizations -------------------------------------------------------------------------

drop policy if exists "read own org" on public.organizations;
create policy "read own org" on public.organizations
  for select
  using (id in (select public.auth_org_ids()));

-- store_access --------------------------------------------------------------------------

drop policy if exists "owners manage store access" on public.store_access;
create policy "owners manage store access" on public.store_access
  for all
  using (
    store_id in (
      select s.id from public.stores s
      where s.organization_id in (select public.auth_owner_org_ids())
    )
  )
  with check (
    store_id in (
      select s.id from public.stores s
      where s.organization_id in (select public.auth_owner_org_ids())
    )
  );

commit;
