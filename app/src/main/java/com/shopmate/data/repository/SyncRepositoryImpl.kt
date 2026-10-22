package com.shopmate.data.repository

import com.shopmate.data.local.dao.SaleDao
import com.shopmate.data.local.dao.SyncEventDao
import com.shopmate.data.local.entity.SyncEventEntity
import com.shopmate.data.mapper.toDomain
import com.shopmate.domain.model.Sale
import com.shopmate.domain.model.EventType
import com.shopmate.domain.model.SyncEntry
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
    private val saleDao: SaleDao,
) : SyncRepository {

    override fun getRecentEntries(limit: Int): Flow<List<SyncEntry>> =
        syncEventDao.getRecentWithSale(limit).map { rows ->
            rows.map { row ->
                SyncEntry(
                    event = SyncEvent(
                        id = row.id,
                        eventType = EventType.valueOf(row.eventType),
                        payload = row.payload,
                        createdAt = Instant.ofEpochMilli(row.createdAt),
                        synced = row.synced,
                        failed = row.failed,
                        errorMessage = row.errorMessage,
                    ),
                    sale = if (row.receiptNumber != null && row.totalAmount != null) {
                        SyncEntry.SaleSummary(
                            receiptNumber = row.receiptNumber,
                            total = row.totalAmount,
                            originalReceiptNumber = row.originalReceiptNumber,
                            sentAt = row.saleSyncedAt,
                        )
                    } else {
                        null
                    },
                )
            }
        }

    override fun lastSentAt(): Flow<Instant?> =
        saleDao.lastSyncedAt().map { it?.let(Instant::ofEpochMilli) }

    override suspend fun retry(eventId: String) =
        syncEventDao.retry(eventId)

    override fun getPendingCount(): Flow<Int> =
        syncEventDao.getPendingCount()

    override fun getFailedCount(): Flow<Int> =
        syncEventDao.getFailedCount()

    override suspend fun retryFailed() =
        syncEventDao.retryFailed()

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
