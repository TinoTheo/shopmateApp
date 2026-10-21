package com.shopmate.data.remote.mapper

import com.shopmate.data.local.entity.StockDeltaEntity
import com.shopmate.data.remote.dto.CategorySnapshotDto
import com.shopmate.data.remote.dto.CategorySnapshotSbDto
import com.shopmate.data.remote.dto.ProductSnapshotDto
import com.shopmate.data.remote.dto.ProductSnapshotSbDto
import com.shopmate.data.remote.dto.SaleDto
import com.shopmate.data.remote.dto.SaleItemDto
import com.shopmate.data.remote.dto.SaleItemRowDto
import com.shopmate.data.remote.dto.SaleRowDto
import com.shopmate.data.remote.dto.ServerProductDto
import com.shopmate.data.remote.dto.StockDeltaRowDto
import com.shopmate.data.remote.dto.SupabaseProductDto
import com.shopmate.domain.model.Category
import com.shopmate.domain.model.Product
import com.shopmate.domain.model.Sale
import com.shopmate.domain.model.SaleItem
import java.time.Instant

fun Sale.toDto(): SaleDto = SaleDto(
    id = id,
    receiptNumber = receiptNumber,
    items = items.map { it.toDto() },
    totalAmount = totalAmount.toPlainString(),
    paymentMethod = paymentMethod.name,
    cashierName = cashierName,
    createdAt = createdAt.toEpochMilli(),
)

fun SaleItem.toDto(): SaleItemDto = SaleItemDto(
    productId = productId,
    productName = productName,
    categoryName = categoryName,
    unitPrice = unitPrice.toPlainString(),
    quantity = quantity.toInt(),
    lineTotal = lineTotal.toPlainString(),
)

fun Product.toSnapshotDto(categoryName: String?): ProductSnapshotDto = ProductSnapshotDto(
    id = id,
    name = name,
    sku = sku,
    categoryId = categoryId,
    categoryName = categoryName,
    sellingPrice = sellingPrice.toPlainString(),
    costPrice = costPrice.toPlainString(),
    stockQuantity = stockQuantity,
    lowStockThreshold = lowStockThreshold,
    unit = unit,
)

fun Category.toSnapshotDto(): CategorySnapshotDto = CategorySnapshotDto(
    id = id,
    name = name,
    colour = colour,
)

// === Supabase postgrest rows ===

fun Sale.toSaleRow(storeId: String): SaleRowDto = SaleRowDto(
    id = id,
    storeId = storeId,
    receiptNumber = receiptNumber,
    totalAmount = totalAmount.toPlainString(),
    paymentMethod = paymentMethod.name,
    cashierName = cashierName,
    createdAt = createdAt.toString(),
)

fun Sale.toSaleItemRows(): List<SaleItemRowDto> = items.map { item ->
    SaleItemRowDto(
        id = item.id,
        saleId = id,
        productId = item.productId,
        productName = item.productName,
        categoryName = item.categoryName,
        unitPrice = item.unitPrice.toPlainString(),
        quantity = item.quantity.toPlainString(),
        lineTotal = item.lineTotal.toPlainString(),
    )
}

fun StockDeltaEntity.toRow(storeId: String): StockDeltaRowDto = StockDeltaRowDto(
    id = id,
    storeId = storeId,
    productId = productId,
    delta = delta,
    reason = reason.name,
    note = note,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
)

fun Product.toSnapshotSbDto(categoryName: String?): ProductSnapshotSbDto = ProductSnapshotSbDto(
    id = id,
    name = name,
    sku = sku,
    categoryId = categoryId,
    categoryName = categoryName,
    sellingPrice = sellingPrice.toPlainString(),
    costPrice = costPrice.toPlainString(),
    stockQuantity = stockQuantity,
    lowStockThreshold = lowStockThreshold,
    unit = unit,
)

fun Category.toSnapshotSbDto(): CategorySnapshotSbDto = CategorySnapshotSbDto(
    id = id,
    name = name,
    colour = colour,
)

/** Adapts a pulled Supabase product row to the shape [com.shopmate.sync.ConflictResolver] expects. */
fun SupabaseProductDto.toServerProductDto(): ServerProductDto = ServerProductDto(
    id = id,
    name = name,
    sellingPrice = sellingPrice,
    costPrice = costPrice,
    stockQuantity = stockQuantity,
    updatedAt = updatedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        ?: Instant.now().toEpochMilli(),
)
