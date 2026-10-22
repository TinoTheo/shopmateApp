package com.shopmate.data.remote

import com.shopmate.data.local.entity.StockDeltaEntity
import com.shopmate.data.remote.dto.StoreSnapshotRowDto
import com.shopmate.data.remote.dto.SupabaseProductDto
import com.shopmate.data.remote.mapper.toRow
import com.shopmate.data.remote.mapper.toSaleItemRows
import com.shopmate.data.remote.mapper.toSaleRow
import com.shopmate.domain.model.Sale
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The whole write surface the sync workers use. Uploads are idempotent upserts keyed on the
 * client-generated row id with `ignoreDuplicates` — ON CONFLICT DO NOTHING, so still insert-only
 * against RLS, and a retried sale never double-posts.
 *
 * Failures propagate as supabase-kt `RestException`s; the workers decide what a status means.
 */
@Singleton
class ShopMateRemote @Inject constructor(
    private val postgrest: Postgrest,
) {

    /** Sale headers first, then their lines — `sale_items` RLS scopes through the parent sale. */
    suspend fun upsertSales(sales: List<Sale>, storeId: String) {
        if (sales.isEmpty()) return
        postgrest.from("sales").upsert(sales.map { it.toSaleRow(storeId) }) {
            ignoreDuplicates = true
        }
        postgrest.from("sale_items").upsert(sales.flatMap { it.toSaleItemRows() }) {
            ignoreDuplicates = true
        }
    }

    suspend fun upsertStockDeltas(deltas: List<StockDeltaEntity>, storeId: String) {
        if (deltas.isEmpty()) return
        postgrest.from("stock_deltas").upsert(deltas.map { it.toRow(storeId) }) {
            ignoreDuplicates = true
        }
    }

    /** Snapshots are point-in-time and append-only; a duplicate costs a row, never a wrong total. */
    suspend fun insertSnapshot(row: StoreSnapshotRowDto) {
        postgrest.from("store_snapshots").insert(row)
    }

    /**
     * Product metadata for catalogue reconciliation. Numeric columns are cast to text
     * server-side so BigDecimal strings survive the round trip.
     *
     * RLS scopes to the org; the `store_id` filter narrows to the store this till works for
     * (CLAUDE.md § Backend & sync — narrow, never widen). `store_id` is selected too, so
     * [com.shopmate.sync.ConflictResolver] can check every row again.
     */
    suspend fun fetchProducts(storeId: String): List<SupabaseProductDto> =
        postgrest.from("products")
            .select(Columns.raw("id,store_id,name,selling_price::text,cost_price::text,stock_quantity,updated_at")) {
                filter { eq("store_id", storeId) }
            }
            .decodeList<SupabaseProductDto>()
}
