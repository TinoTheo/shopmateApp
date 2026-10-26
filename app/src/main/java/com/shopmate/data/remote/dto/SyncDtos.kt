package com.shopmate.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// kotlinx.serialization rows for the Supabase PostgREST sync target. Column names map 1:1
// to the cloud schema (snake_case). Money travels as strings (Postgres casts text → numeric,
// and numeric → JSON would lose BigDecimal precision on the way back); timestamps as ISO-8601.

// === STORES ===

@Serializable
data class StoreDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("currency_symbol") val currencySymbol: String? = null,
)

// === SALES (insert-only, matches RLS) ===

@Serializable
data class SaleRowDto(
    @SerialName("id") val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("receipt_number") val receiptNumber: String,
    @SerialName("total_amount") val totalAmount: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("cashier_name") val cashierName: String?,
    @SerialName("created_at") val createdAt: String,
    /** Left out entirely when null, so a till without staff works before the staff migration. */
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("staff_id") val staffId: String? = null,
)

// No store_id column — RLS scopes sale_items through the parent sale (sale_id).
@Serializable
data class SaleItemRowDto(
    @SerialName("id") val id: String,
    @SerialName("sale_id") val saleId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("category_name") val categoryName: String?,
    @SerialName("unit_price") val unitPrice: String,
    @SerialName("quantity") val quantity: String,
    @SerialName("line_total") val lineTotal: String,
)

// === STOCK DELTAS ===

@Serializable
data class StockDeltaRowDto(
    @SerialName("id") val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("delta") val delta: Int,
    @SerialName("reason") val reason: String,
    @SerialName("note") val note: String?,
    @SerialName("created_at") val createdAt: String,
    /** Who made it (§ Staff); left out when null. */
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("actor") val actor: String? = null,
)

// === MULTI-TILL (§ Multi-till): the store's ledger and catalogue, shared through the server ===

/** Another till's movement, as the server holds it. [receivedAt] is the server's clock. */
@Serializable
data class LedgerRowDto(
    @SerialName("id") val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("delta") val delta: Int,
    @SerialName("reason") val reason: String,
    @SerialName("note") val note: String? = null,
    @SerialName("actor") val actor: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("received_at") val receivedAt: String,
)

/** A product row both ways. Money as text (see the file header); stock is informational only. */
@Serializable
data class CatalogueProductDto(
    @SerialName("id") val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("sku") val sku: String? = null,
    @SerialName("barcode") val barcode: String? = null,
    @SerialName("selling_price") val sellingPrice: String,
    @SerialName("cost_price") val costPrice: String? = null,
    @SerialName("stock_quantity") val stockQuantity: Int = 0,
    @SerialName("low_stock_threshold") val lowStockThreshold: Int,
    @SerialName("unit") val unit: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class CatalogueCategoryDto(
    @SerialName("id") val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("name") val name: String,
    @SerialName("colour") val colour: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

// === STORE SNAPSHOTS (products + categories inside one jsonb `payload` column) ===

@Serializable
data class StoreSnapshotRowDto(
    @SerialName("store_id") val storeId: String,
    @SerialName("taken_at") val takenAt: String,
    @SerialName("payload") val payload: SnapshotPayload,
)

@Serializable
data class SnapshotPayload(
    @SerialName("products") val products: List<ProductSnapshotSbDto>,
    @SerialName("categories") val categories: List<CategorySnapshotSbDto>,
)

@Serializable
data class ProductSnapshotSbDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("sku") val sku: String?,
    @SerialName("category_id") val categoryId: String?,
    @SerialName("category_name") val categoryName: String?,
    @SerialName("selling_price") val sellingPrice: String,
    @SerialName("cost_price") val costPrice: String?,
    @SerialName("stock_quantity") val stockQuantity: Int,
    @SerialName("low_stock_threshold") val lowStockThreshold: Int,
    @SerialName("unit") val unit: String,
)

@Serializable
data class CategorySnapshotSbDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("colour") val colour: String?,
)

