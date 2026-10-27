-- A removed till's stop, confirmed (CLAUDE.md § Devices; dashboard handoff 1m).
--
-- Removing a till sets revoked_at; the till learns it at its next sync and stops. Until now the
-- server never heard that it had stopped, so the dashboard could only say "Stops at its next sync"
-- forever. till_seen now records the first time a removed till checks in — that is the moment it
-- stops — in stopped_at, and the dashboard shows "Stopped at 15:12".
--
-- Safe to run more than once.

begin;

alter table public.tills add column if not exists stopped_at timestamptz;

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
    -- The till hears it was removed and stops: the first time is when it stopped.
    update tills set stopped_at = coalesce(stopped_at, now()) where id = v_till.id;
    return 'revoked';
  end if;
  update tills set last_seen_at = now() where id = v_till.id;
  return 'active';
end;
$$;

revoke all on function public.till_seen(uuid, text) from public;
grant execute on function public.till_seen(uuid, text) to authenticated;

commit;
