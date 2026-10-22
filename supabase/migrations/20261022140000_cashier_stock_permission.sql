-- Who may move stock other than by selling.
--
-- Owners and managers add, adjust and count stock. A cashier may only if the owner grants it
-- for a store — the per-store grant lives on that cashier's store_access row. Selling is open
-- to every member, as before.
--
-- To grant a cashier stock rights at one store (run as the owner, or in the SQL editor):
--
--   update public.store_access sa
--   set can_manage_stock = true
--   from public.memberships m
--   where m.id = sa.membership_id
--     and m.user_id = '<cashier auth user id>'
--     and sa.store_id = '<store id>';
--
-- The app reads the result through auth_can_manage_stock(store_id) at sign-in and hides
-- restock, adjust, count and opening stock when it is false, so a cashier without the grant
-- never makes a movement the server will refuse.
--
-- Safe to re-run.

begin;

alter table public.store_access
  add column if not exists can_manage_stock boolean not null default false;

-- True when the signed-in user may record non-sale stock movements at [p_store_id]: an owner or
-- manager of its org, or a member whose store_access row for it carries the grant.
-- SECURITY DEFINER for the same reason as auth_org_ids(): it reads memberships/store_access
-- without re-applying their policies, and only ever answers for auth.uid().
create or replace function public.auth_can_manage_stock(p_store_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.auth_has_role(p_store_id, array['owner', 'manager']::public.member_role[])
      or exists (
        select 1
        from public.store_access sa
        join public.memberships m on m.id = sa.membership_id
        where sa.store_id = p_store_id
          and m.user_id = auth.uid()
          and sa.can_manage_stock
      )
$$;

revoke all on function public.auth_can_manage_stock(uuid) from public;
grant execute on function public.auth_can_manage_stock(uuid) to authenticated;

-- stock_deltas: SALE from any member of the store; anything else needs the stock right.
drop policy if exists "insert stock deltas" on public.stock_deltas;
create policy "insert stock deltas" on public.stock_deltas
  for insert
  with check (
    store_id in (select public.auth_accessible_store_ids())
    and (reason = 'SALE' or public.auth_can_manage_stock(store_id))
  );

commit;
