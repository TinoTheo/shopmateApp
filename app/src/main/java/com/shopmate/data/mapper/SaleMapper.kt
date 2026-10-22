package com.shopmate.data.mapper

import com.shopmate.data.local.dao.SaleWithItems
import com.shopmate.data.local.entity.SaleEntity
import com.shopmate.data.local.entity.SaleItemEntity
import com.shopmate.domain.model.Sale
import com.shopmate.domain.model.SaleItem

fun SaleWithItems.toDomain(): Sale = Sale(
    id = sale.id,
    receiptNumber = sale.receiptNumber,
    totalAmount = sale.totalAmount,
    paymentMethod = sale.paymentMethod,
    cashierName = sale.cashierName,
    items = items.map { it.toDomain() },
    createdAt = sale.createdAt,
    syncedAt = sale.syncedAt,
    originalReceiptNumber = sale.originalReceiptNumber,
)

fun SaleItemEntity.toDomain(): SaleItem = SaleItem(
    id = id,
    productId = productId,
    productName = productName,
    categoryName = categoryName,
    unitPrice = unitPrice,
    quantity = quantity,
    lineTotal = lineTotal,
)

fun Sale.toEntity(): SaleEntity = SaleEntity(
    id = id,
    receiptNumber = receiptNumber,
    totalAmount = totalAmount,
    paymentMethod = paymentMethod,
    cashierName = cashierName,
    createdAt = createdAt,
    syncedAt = syncedAt,
    originalReceiptNumber = originalReceiptNumber,
)

fun SaleItem.toEntity(saleId: String): SaleItemEntity = SaleItemEntity(
    id = id,
    saleId = saleId,
    productId = productId,
    productName = productName,
    categoryName = categoryName,
    unitPrice = unitPrice,
    quantity = quantity,
    lineTotal = lineTotal,
)
