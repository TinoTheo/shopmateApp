package com.shopmate.data.repository

import androidx.room.withTransaction
import com.shopmate.data.local.AppDatabase
import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.local.dao.StockDeltaDao
import com.shopmate.data.local.dao.SyncEventDao
import com.shopmate.data.local.entity.StockDeltaEntity
import com.shopmate.data.local.entity.SyncEventEntity
import com.shopmate.data.mapper.toDomain
import com.shopmate.data.mapper.toEntity
import com.shopmate.domain.model.DeltaReason
import com.shopmate.domain.model.Product
import com.shopmate.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val stockDeltaDao: StockDeltaDao,
    private val syncEventDao: SyncEventDao,
) : ProductRepository {

    override fun getAll(): Flow<List<Product>> =
        productDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Product? =
        productDao.getById(id)?.toDomain()

    override suspend fun add(product: Product) =
        productDao.insert(product.toEntity())

    override suspend fun update(product: Product) =
        productDao.update(product.toEntity())

    /**
     * Stock is only ever changed by a signed delta. One atomic transaction:
     * 1. increment the product's stock, 2. append to the stock_deltas ledger,
     * 3. queue a sync event so the change uploads later.
     */
    override suspend fun adjustStock(
        productId: String,
        delta: Int,
        reason: DeltaReason,
        note: String?,
        countSessionId: String?,
    ) = db.withTransaction {
        productDao.incrementStock(productId, delta)
        stockDeltaDao.insert(
            StockDeltaEntity(
                id = UUID.randomUUID().toString(),
                productId = productId,
                delta = delta,
                reason = reason,
                note = note,
                countSessionId = countSessionId,
                // Stays null until auth ships — never a placeholder (CLAUDE.md § Schema debt).
                actor = null,
                createdAt = Instant.now().toEpochMilli(),
                synced = false,
            )
        )
        syncEventDao.insert(
            SyncEventEntity(
                id = UUID.randomUUID().toString(),
                eventType = "SNAPSHOT",
                payload = productId,
                createdAt = Instant.now().toEpochMilli(),
                synced = false,
                failed = false,
                errorMessage = null,
            )
        )
    }

    override fun search(query: String): Flow<List<Product>> =
        productDao.search(query).map { list -> list.map { it.toDomain() } }
}
