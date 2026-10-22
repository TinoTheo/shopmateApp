package com.shopmate.data.remote.dto

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

// === SERVER PRODUCTS (pull side; numeric columns cast to text in the select) ===

@Serializable
data class SupabaseProductDto(
    @SerialName("id") val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("name") val name: String,
    @SerialName("selling_price") val sellingPrice: String,
    @SerialName("cost_price") val costPrice: String? = null,
    @SerialName("stock_quantity") val stockQuantity: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
)

// === CONFLICT RESOLUTION (local shape, not a wire row) ===

/** A pulled product, with `updated_at` resolved to epoch millis for [com.shopmate.sync.ConflictResolver]. */
data class ServerProductDto(
    val id: String,
    val storeId: String,
    val name: String,
    val sellingPrice: String,
    val costPrice: String?,
    val stockQuantity: Int,
    val updatedAt: Long,
)
