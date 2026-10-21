package com.shopmate.data.mapper

import com.shopmate.data.local.entity.StockDeltaEntity
import com.shopmate.domain.model.StockDelta
import java.time.Instant

fun StockDeltaEntity.toDomain(): StockDelta = StockDelta(
    id = id,
    productId = productId,
    delta = delta,
    reason = reason,
    note = note,
    countSessionId = countSessionId,
    actor = actor,
    createdAt = Instant.ofEpochMilli(createdAt),
    synced = synced,
)

fun StockDelta.toEntity(): StockDeltaEntity = StockDeltaEntity(
    id = id,
    productId = productId,
    delta = delta,
    reason = reason,
    note = note,
    countSessionId = countSessionId,
    actor = actor,
    createdAt = createdAt.toEpochMilli(),
    synced = synced,
)
