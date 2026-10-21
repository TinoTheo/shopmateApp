package com.shopmate.data.mapper

import com.shopmate.data.local.entity.SyncEventEntity
import com.shopmate.domain.model.EventType
import com.shopmate.domain.model.SyncEvent
import java.time.Instant

fun SyncEventEntity.toDomain(): SyncEvent = SyncEvent(
    id = id,
    eventType = EventType.valueOf(eventType),
    payload = payload,
    createdAt = Instant.ofEpochMilli(createdAt),
    synced = synced,
    failed = failed,
    errorMessage = errorMessage,
)
