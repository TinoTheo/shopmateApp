package com.shopmate.data.remote.mapper

import com.shopmate.data.local.entity.StockDeltaEntity
import com.shopmate.data.remote.dto.CategorySnapshotSbDto
import com.shopmate.data.remote.dto.ProductSnapshotSbDto
import com.shopmate.data.remote.dto.SaleItemRowDto
import com.shopmate.data.remote.dto.SaleRowDto
import com.shopmate.data.remote.dto.StockDeltaRowDto
import com.shopmate.domain.model.Category
import com.shopmate.domain.model.Product
import com.shopmate.domain.model.Sale
import java.time.Instant

// === Supabase PostgREST rows ===

fun Sale.toSaleRow(storeId: String): SaleRowDto = SaleRowDto(
    id = id,
    storeId = storeId,
    receiptNumber = receiptNumber,
    totalAmount = totalAmount.toPlainString(),
    paymentMethod = paymentMethod.name,
    cashierName = cashierName,
    createdAt = createdAt.toString(),
    staffId = staffId,
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
    actor = actor,
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

