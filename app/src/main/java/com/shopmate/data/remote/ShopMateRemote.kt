package com.shopmate.data.remote

import com.shopmate.data.local.entity.StockDeltaEntity
import com.shopmate.data.remote.dto.CatalogueCategoryDto
import com.shopmate.data.remote.dto.CatalogueProductDto
import com.shopmate.data.remote.dto.LedgerRowDto
import com.shopmate.data.remote.dto.StoreSnapshotRowDto
import com.shopmate.data.remote.mapper.toRow
import com.shopmate.data.remote.mapper.toSaleItemRows
import com.shopmate.data.remote.mapper.toSaleRow
import com.shopmate.domain.model.Sale
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
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
     * The store's movements the server received at or after [since] (its own clock), oldest
     * first, one page. Includes this till's own — the caller skips what it already holds.
     */
    suspend fun fetchLedgerPage(storeId: String, since: String?, from: Long, pageSize: Long): List<LedgerRowDto> =
        postgrest.from("stock_deltas")
            .select(Columns.list("id", "store_id", "product_id", "delta", "reason", "note", "actor", "created_at", "received_at")) {
                filter {
                    eq("store_id", storeId)
                    if (since != null) gte("received_at", since)
                }
                order("received_at", Order.ASCENDING)
                order("id", Order.ASCENDING)
                range(from, from + pageSize - 1)
            }
            .decodeList<LedgerRowDto>()

    /** Every product of the store, one page, money cast to text so BigDecimal survives. */
    suspend fun fetchCataloguePage(storeId: String, from: Long, pageSize: Long): List<CatalogueProductDto> =
        postgrest.from("products")
            .select(
                Columns.raw(
                    "id,store_id,category_id,name,sku,barcode,selling_price::text,cost_price::text," +
                        "stock_quantity,low_stock_threshold,unit,created_at,updated_at",
                ),
            ) {
                filter { eq("store_id", storeId) }
                order("id", Order.ASCENDING)
                range(from, from + pageSize - 1)
            }
            .decodeList<CatalogueProductDto>()

    suspend fun fetchCategories(storeId: String): List<CatalogueCategoryDto> =
        postgrest.from("categories")
            .select { filter { eq("store_id", storeId) } }
            .decodeList<CatalogueCategoryDto>()

    /** Newest edit wins on the server (`products_keep_newest`), so an older upload is ignored. */
    suspend fun upsertProducts(rows: List<CatalogueProductDto>) {
        if (rows.isEmpty()) return
        postgrest.from("products").upsert(rows)
    }

    suspend fun upsertCategories(rows: List<CatalogueCategoryDto>) {
        if (rows.isEmpty()) return
        postgrest.from("categories").upsert(rows)
    }


}
