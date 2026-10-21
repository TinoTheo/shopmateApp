package com.shopmate.data.repository

import androidx.room.withTransaction
import com.shopmate.data.local.AppDatabase
import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.local.dao.SaleDao
import com.shopmate.data.local.dao.StockDeltaDao
import com.shopmate.data.local.dao.SyncEventDao
import com.shopmate.data.local.entity.StockDeltaEntity
import com.shopmate.data.local.entity.SyncEventEntity
import com.shopmate.data.mapper.toDomain
import com.shopmate.data.mapper.toEntity
import com.shopmate.domain.model.DeltaReason
import com.shopmate.domain.model.Sale
import com.shopmate.domain.repository.SaleRepository
import com.shopmate.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val saleDao: SaleDao,
    private val productDao: ProductDao,
    private val stockDeltaDao: StockDeltaDao,
    private val syncEventDao: SyncEventDao,
    private val syncScheduler: SyncScheduler,
) : SaleRepository {

    override suspend fun recordSale(sale: Sale) {
        db.withTransaction { recordSaleTx(sale) }
        // Fire-and-forget immediate upload attempt; the SALE event queued above is the fallback.
        syncScheduler.syncSaleImmediately(sale.id)
    }

    private suspend fun recordSaleTx(sale: Sale) {
        saleDao.insertSaleWithItems(
            sale.toEntity(),
            sale.items.map { it.toEntity(sale.id) },
        )
        sale.items.forEach { item ->
            val soldQty = item.quantity.toInt()
            productDao.incrementStock(item.productId, -soldQty)
            // Record the sale as a negative SALE delta so it shows in the stock ledger/activity feed.
            stockDeltaDao.insert(
                StockDeltaEntity(
                    id = UUID.randomUUID().toString(),
                    productId = item.productId,
                    delta = -soldQty,
                    reason = DeltaReason.SALE,
                    note = sale.receiptNumber,
                    createdAt = sale.createdAt.toEpochMilli(),
                    synced = false,
                )
            )
        }
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

    override fun getSalesByDateRange(start: Instant, end: Instant): Flow<List<Sale>> =
        saleDao.getByDateRange(start.toEpochMilli(), end.toEpochMilli())
            .map { list -> list.map { it.toDomain() } }

    override fun getRecentSales(limit: Int): Flow<List<Sale>> =
        saleDao.getRecent(limit).map { list -> list.map { it.toDomain() } }
}
