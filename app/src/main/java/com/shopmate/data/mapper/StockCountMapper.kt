package com.shopmate.data.mapper

import com.shopmate.data.local.entity.StockCountEntity
import com.shopmate.domain.model.StockCount
import java.time.Instant

fun StockCountEntity.toDomain(): StockCount = StockCount(
    id = id,
    startedAt = Instant.ofEpochMilli(startedAt),
    committedAt = committedAt?.let(Instant::ofEpochMilli),
    countedBy = countedBy,
    itemCount = itemCount,
    varianceCount = varianceCount,
    varianceValue = varianceValue,
)

fun StockCount.toEntity(): StockCountEntity = StockCountEntity(
    id = id,
    startedAt = startedAt.toEpochMilli(),
    committedAt = committedAt?.toEpochMilli(),
    countedBy = countedBy,
    itemCount = itemCount,
    varianceCount = varianceCount,
    varianceValue = varianceValue,
)
