-- Multi-till (CLAUDE.md § Multi-till): several tills share one store's ledger and catalogue.
--
--   stock_deltas.received_at   when the server got the movement — tills pull other tills'
--                              movements by this, never by the till's own clock
--   stock_deltas.actor         who made it, so every till's ledger can say
--   members read stock deltas  every till of the store reads the store's ledger
--   products_keep_newest       an upload older than the server's row is ignored, so a till
--                              holding a stale price can't overwrite a newer one
--
-- Safe to re-run.

begin;

alter table public.stock_deltas add column if not exists received_at timestamptz not null default now();
alter table public.stock_deltas add column if not exists actor text;

create index if not exists stock_deltas_store_received
  on public.stock_deltas (store_id, received_at, id);

-- The server's clock, always: a client-sent received_at would let a till hide movements from
-- the others' incremental pulls.
create or replace function public.stock_deltas_received_now()
returns trigger
language plpgsql
as $$
begin
  new.received_at := now();
  return new;
end;
$$;

drop trigger if exists stock_deltas_received_now on public.stock_deltas;
create trigger stock_deltas_received_now
  before insert on public.stock_deltas
  for each row execute function public.stock_deltas_received_now();

drop policy if exists "members read stock deltas" on public.stock_deltas;
create policy "members read stock deltas" on public.stock_deltas
  for select
  using (store_id in (select public.auth_accessible_store_ids()));

-- Newest edit wins: an update carrying an older updated_at than the row has leaves it alone.
create or replace function public.products_keep_newest()
returns trigger
language plpgsql
as $$
begin
  if new.updated_at < old.updated_at then
    return null;
  end if;
  return new;
end;
$$;

drop trigger if exists products_keep_newest on public.products;
create trigger products_keep_newest
  before update on public.products
  for each row execute function public.products_keep_newest();

commit;
