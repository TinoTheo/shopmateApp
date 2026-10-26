import { selectAll, supabase } from './supabase';
import { tillCodeOf } from './format';

export type Store = { id: string; name: string; currency_symbol: string };
export type Till = { id: string; code: string; name: string; last_seen_at: string };
export type Sale = {
  id: string;
  receipt_number: string;
  total_amount: number;
  payment_method: string;
  cashier_name: string | null;
  created_at: string;
};
export type Line = { product_name: string; quantity: number; line_total: number };

/** The stores this owner can see — RLS decides, as in the till app. */
export async function stores(): Promise<Store[]> {
  const { data, error } = await supabase.from('stores').select('id,name,currency_symbol').order('name');
  if (error) throw new Error(error.message);
  return data ?? [];
}

export async function tills(storeId: string): Promise<Till[]> {
  const { data, error } = await supabase
    .from('tills')
    .select('id,code,name,last_seen_at')
    .eq('store_id', storeId)
    .is('revoked_at', null)
    .order('last_seen_at', { ascending: false });
  if (error) throw new Error(error.message);
  return data ?? [];
}

export async function sales(storeId: string, from: Date, to: Date): Promise<Sale[]> {
  const rows = await selectAll<Sale>((a, b) =>
    supabase
      .from('sales')
      .select('id,receipt_number,total_amount,payment_method,cashier_name,created_at')
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
    const ids = saleIds.slice(i, i + 200);
    const { data, error } = await supabase
      .from('sale_items')
      .select('product_name,quantity,line_total')
      .in('sale_id', ids);
    if (error) throw new Error(error.message);
    out.push(...(data ?? []).map((l) => ({ ...l, quantity: Number(l.quantity), line_total: Number(l.line_total) })));
  }
  return out;
}

export type StockRow = { productId: string; name: string; stock: number; movements: number; lastMovedAt: string | null };

/**
 * Stock as the store's whole ledger — the sum of every till's movements (CLAUDE.md § Multi-till),
 * never a till's own number. Movements for a product not in the catalogue are left out.
 */
export async function stock(storeId: string): Promise<StockRow[]> {
  const products = await selectAll<{ id: string; name: string }>((a, b) =>
    supabase.from('products').select('id,name').eq('store_id', storeId).range(a, b),
  );
  const deltas = await selectAll<{ product_id: string; delta: number; created_at: string }>((a, b) =>
    supabase.from('stock_deltas').select('product_id,delta,created_at').eq('store_id', storeId).range(a, b),
  );
  const byProduct = new Map<string, { stock: number; movements: number; last: string | null }>();
  for (const d of deltas) {
    const e = byProduct.get(d.product_id) ?? { stock: 0, movements: 0, last: null };
    e.stock += d.delta;
    e.movements += 1;
    if (!e.last || d.created_at > e.last) e.last = d.created_at;
    byProduct.set(d.product_id, e);
  }
  return products
    .map((p) => {
      const e = byProduct.get(p.id);
      return { productId: p.id, name: p.name, stock: e?.stock ?? 0, movements: e?.movements ?? 0, lastMovedAt: e?.last ?? null };
    })
    .sort((a, b) => a.stock - b.stock || a.name.localeCompare(b.name));
}

export type Movement = { delta: number; reason: string; note: string | null; actor: string | null; created_at: string };

/** "Why does it say 6?" — one product's ledger, newest first, with who made each movement. */
export async function ledger(storeId: string, productId: string): Promise<Movement[]> {
  return selectAll<Movement>((a, b) =>
    supabase
      .from('stock_deltas')
      .select('delta,reason,note,actor,created_at')
      .eq('store_id', storeId)
      .eq('product_id', productId)
      .order('created_at', { ascending: false })
      .range(a, b),
  );
}

/** Totals by till code, for "which till took what". */
export function byTill(rows: Sale[]): Map<string, { count: number; total: number }> {
  const m = new Map<string, { count: number; total: number }>();
  for (const s of rows) {
    const code = tillCodeOf(s.receipt_number) ?? '—';
    const e = m.get(code) ?? { count: 0, total: 0 };
    e.count += 1;
    e.total += s.total_amount;
    m.set(code, e);
  }
  return m;
}
