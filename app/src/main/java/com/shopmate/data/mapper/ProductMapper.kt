package com.shopmate.data.mapper

import com.shopmate.data.local.entity.ProductEntity
import com.shopmate.domain.model.Product

fun ProductEntity.toDomain(): Product = Product(
    id = id,
    name = name,
    sku = sku,
    barcode = barcode,
    categoryId = categoryId,
    sellingPrice = sellingPrice,
    costPrice = costPrice,
    previousCostPrice = previousCostPrice,
    costPriceChangedAt = costPriceChangedAt,
    sellingPriceSetAt = sellingPriceSetAt,
    supplier = supplier,
    stockQuantity = stockQuantity,
    lowStockThreshold = lowStockThreshold,
    unit = unit,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    name = name,
    sku = sku,
    barcode = barcode,
    categoryId = categoryId,
    sellingPrice = sellingPrice,
    costPrice = costPrice,
    previousCostPrice = previousCostPrice,
    costPriceChangedAt = costPriceChangedAt,
    sellingPriceSetAt = sellingPriceSetAt,
    supplier = supplier,
    stockQuantity = stockQuantity,
    lowStockThreshold = lowStockThreshold,
    unit = unit,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
