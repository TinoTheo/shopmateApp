package com.shopmate.data.remote.dto

import com.squareup.moshi.Json

// === SALE SYNC ===

data class SyncSalesRequest(
    @Json(name = "store_id") val storeId: String,
    @Json(name = "sales") val sales: List<SaleDto>,
)

data class SaleDto(
    @Json(name = "id") val id: String,
    @Json(name = "receipt_number") val receiptNumber: String,
    @Json(name = "items") val items: List<SaleItemDto>,
    @Json(name = "total_amount") val totalAmount: String,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "cashier_name") val cashierName: String?,
    @Json(name = "created_at") val createdAt: Long,
)

data class SaleItemDto(
    @Json(name = "product_id") val productId: String,
    @Json(name = "product_name") val productName: String,
    @Json(name = "category_name") val categoryName: String?,
    @Json(name = "unit_price") val unitPrice: String,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "line_total") val lineTotal: String,
)

data class SyncSalesResponse(
    @Json(name = "synced_count") val syncedCount: Int,
    @Json(name = "failed_ids") val failedIds: List<String>,
)

// === STORE SNAPSHOT ===

data class StoreSnapshotRequest(
    @Json(name = "store_id") val storeId: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "products") val products: List<ProductSnapshotDto>,
    @Json(name = "categories") val categories: List<CategorySnapshotDto>,
)

data class ProductSnapshotDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "sku") val sku: String?,
    @Json(name = "category_id") val categoryId: String?,
    @Json(name = "category_name") val categoryName: String?,
    @Json(name = "selling_price") val sellingPrice: String,
    @Json(name = "cost_price") val costPrice: String?,
    @Json(name = "stock_quantity") val stockQuantity: Int,
    @Json(name = "low_stock_threshold") val lowStockThreshold: Int,
    @Json(name = "unit") val unit: String,
)

data class CategorySnapshotDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "colour") val colour: String?,
)

data class StoreSnapshotResponse(
    @Json(name = "received") val received: Boolean,
)

// === SERVER PRODUCTS (for conflict resolution) ===

data class ServerProductsResponse(
    @Json(name = "products") val products: List<ServerProductDto>,
)

data class ServerProductDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "selling_price") val sellingPrice: String,
    @Json(name = "cost_price") val costPrice: String?,
    @Json(name = "stock_quantity") val stockQuantity: Int,
    @Json(name = "updated_at") val updatedAt: Long,
)
