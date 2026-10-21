package com.shopmate.data.mapper

import com.shopmate.data.local.entity.ProductBarcodeEntity
import com.shopmate.data.local.entity.UnknownBarcodeEntity
import com.shopmate.domain.model.ProductBarcode
import com.shopmate.domain.model.UnknownBarcode
import java.time.Instant

fun ProductBarcodeEntity.toDomain(): ProductBarcode = ProductBarcode(
    id = id,
    productId = productId,
    code = code,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun ProductBarcode.toEntity(): ProductBarcodeEntity = ProductBarcodeEntity(
    id = id,
    productId = productId,
    code = code,
    createdAt = createdAt.toEpochMilli(),
)

fun UnknownBarcodeEntity.toDomain(): UnknownBarcode = UnknownBarcode(
    code = code,
    hits = hits,
    firstSeenAt = Instant.ofEpochMilli(firstSeenAt),
    lastSeenAt = Instant.ofEpochMilli(lastSeenAt),
    lastTill = lastTill,
)

fun UnknownBarcode.toEntity(): UnknownBarcodeEntity = UnknownBarcodeEntity(
    code = code,
    hits = hits,
    firstSeenAt = firstSeenAt.toEpochMilli(),
    lastSeenAt = lastSeenAt.toEpochMilli(),
    lastTill = lastTill,
)
