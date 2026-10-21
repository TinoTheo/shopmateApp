package com.shopmate.domain.model

import java.time.Instant

data class SyncEvent(
    val id: String,
    val eventType: EventType,
    val payload: String,
    val createdAt: Instant,
    val synced: Boolean,
    val failed: Boolean,
    val errorMessage: String?,
)
