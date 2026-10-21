package com.shopmate.data.repository

import com.shopmate.data.local.dao.SyncEventDao
import com.shopmate.data.local.entity.SyncEventEntity
import com.shopmate.data.mapper.toDomain
import com.shopmate.domain.model.Sale
import com.shopmate.domain.model.SyncEvent
import com.shopmate.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncEventDao: SyncEventDao,
) : SyncRepository {

    override fun getPendingCount(): Flow<Int> =
        syncEventDao.getPendingCount()

    override fun getPendingEvents(): Flow<List<SyncEvent>> =
        syncEventDao.getPending().map { list -> list.map { it.toDomain() } }

    override fun getRecentEvents(limit: Int): Flow<List<SyncEvent>> =
        syncEventDao.getRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun markSynced(id: String) =
        syncEventDao.markSynced(id)

    override suspend fun markFailed(id: String, errorMessage: String) =
        syncEventDao.markFailed(id, errorMessage)

    override suspend fun queueSaleEvent(sale: Sale) {
        syncEventDao.insert(
            SyncEventEntity(
                id = UUID.randomUUID().toString(),
                eventType = "SALE",
                payload = sale.id,
                createdAt = Instant.now().toEpochMilli(),
                synced = false,
                failed = false,
                errorMessage = null,
            )
        )
    }

    override suspend fun queueSnapshotEvent() {
        syncEventDao.insert(
            SyncEventEntity(
                id = UUID.randomUUID().toString(),
                eventType = "SNAPSHOT",
                payload = "",
                createdAt = Instant.now().toEpochMilli(),
                synced = false,
                failed = false,
                errorMessage = null,
            )
        )
    }
}
