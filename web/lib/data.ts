import { selectAll, supabase } from './supabase';

// Reads only. Every query is scoped by RLS to the owner's organisation; the dashboard narrows to
// the chosen store and never widens a read (CLAUDE.md § Backend & sync).

export type Store = { id: string; name: string; currency_symbol: string | null };

export type Till = {
  id: string;
  code: string;
  name: string;
  last_seen_at: string;
  revoked_at: string | null;
  /** When a removed till learnt it was removed. Null until it syncs, or before the migration. */
  stopped_at: string | null;
};

export type Sale = {
  id: string;
  receipt_number: string;
  total_amount: number;
  payment_method: string;
  created_at: string;
};

export type Line = { product_id: string; product_name: string; quantity: number; line_total: number };

export type Product = { id: string; name: string; selling_price: number };

export type Movement = {
  id: string;
  product_id: string;
  delta: number;
  reason: string;
  note: string | null;
  actor: string | null;
  created_at: string;
};

function fail(error: { message: string } | null): void {
  if (error) throw new Error(error.message);
}

/** The stores this owner can see — RLS decides, as in the till app. */
export async function stores(): Promise<Store[]> {
  const { data, error } = await supabase.from('stores').select('id,name,currency_symbol').order('name');
  fail(error);
  return data ?? [];
}

/** The owner's name from their till PIN row, for the frame. Null when they haven't set one. */
export async function ownerName(storeId: string): Promise<string | null> {
  const { data } = await supabase.from('staff').select('name').eq('store_id', storeId).eq('role', 'owner').limit(1);
  return data?.[0]?.name ?? null;
}

/** Every till the store has had, removed ones included. */
export async function tills(storeId: string): Promise<Till[]> {
  const base = 'id,code,name,last_seen_at,revoked_at';
  let res = await supabase.from('tills').select(`${base},stopped_at`).eq('store_id', storeId).order('name');
  // Before the stopped-at migration the column doesn't exist; removed tills then read as unconfirmed.
  if (res.error) res = (await supabase.from('tills').select(base).eq('store_id', storeId).order('name')) as typeof res;
  fail(res.error);
  return (res.data ?? []).map((t) => ({ ...t, stopped_at: (t as Partial<Till>).stopped_at ?? null }) as Till);
}

export async function removeTill(id: string): Promise<void> {
  const { error } = await supabase.from('tills').update({ revoked_at: new Date().toISOString() }).eq('id', id);
  fail(error);
}

export async function sales(storeId: string, from: Date, to: Date): Promise<Sale[]> {
  const rows = await selectAll<Sale>((a, b) =>
    supabase
      .from('sales')
      .select('id,receipt_number,total_amount,payment_method,created_at')
      .eq('store_id', storeId)
      .gte('created_at', from.toISOString())
      .lt('created_at', to.toISOString())
      .order('created_at', { ascending: true })
      .range(a, b),
  );
  return rows.map((s) => ({ ...s, total_amount: Number(s.total_amount) }));
}

/** Line items of the given sales, for best sellers. Read in batches of sale ids. */
export async function lines(saleIds: string[]): Promise<Line[]> {
  const out: Line[] = [];
  for (let i = 0; i < saleIds.length; i += 200) {
    const { data, error } = await supabase
      .from('sale_items')
      .select('product_id,product_name,quantity,line_total')
      .in('sale_id', saleIds.slice(i, i + 200));
    fail(error);
    out.push(...(data ?? []).map((l) => ({ ...l, quantity: Number(l.quantity), line_total: Number(l.line_total) })));
  }
  return out;
}

export async function products(storeId: string): Promise<Product[]> {
  const rows = await selectAll<{ id: string; name: string; selling_price: string | number }>((a, b) =>
    supabase.from('products').select('id,name,selling_price').eq('store_id', storeId).order('name').range(a, b),
  );
  return rows.map((p) => ({ ...p, selling_price: Number(p.selling_price) }));
}

/** The store's whole ledger, oldest first — every till's movements (CLAUDE.md § Multi-till). */
export async function movements(storeId: string): Promise<Movement[]> {
  return selectAll<Movement>((a, b) =>
    supabase
      .from('stock_deltas')
      .select('id,product_id,delta,reason,note,actor,created_at')
      .eq('store_id', storeId)
      .order('created_at', { ascending: true })
      .order('id', { ascending: true })
      .range(a, b),
  );
}
