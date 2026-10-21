package com.shopmate.data.repository

import androidx.room.withTransaction
import com.shopmate.data.local.AppDatabase
import com.shopmate.data.local.dao.ProductBarcodeDao
import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.local.dao.UnknownBarcodeDao
import com.shopmate.data.local.entity.ProductBarcodeEntity
import com.shopmate.data.local.entity.UnknownBarcodeEntity
import com.shopmate.data.mapper.toDomain
import com.shopmate.domain.model.Product
import com.shopmate.domain.model.ProductBarcode
import com.shopmate.domain.model.UnknownBarcode
import com.shopmate.domain.repository.BarcodeRepository
import com.shopmate.domain.util.BarcodeNormaliser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BarcodeRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val productBarcodeDao: ProductBarcodeDao,
    private val unknownBarcodeDao: UnknownBarcodeDao,
) : BarcodeRepository {

    override suspend fun lookup(rawCode: String, till: String?): Product? {
        val code = BarcodeNormaliser.normalise(rawCode) ?: return null
        val match = productBarcodeDao.findByCode(code)
        if (match != null) return productDao.getById(match.productId)?.toDomain()
        // A miss is not a failure: record it and let the sale carry on.
        recordSighting(code, till)
        return null
    }

    /** First sighting inserts; a repeat bumps `hits` and `last_seen_at`. */
    private suspend fun recordSighting(code: String, till: String?) {
        val seenAt = Instant.now().toEpochMilli()
        db.withTransaction {
            val existing = unknownBarcodeDao.findByCode(code)
            unknownBarcodeDao.upsert(
                existing?.copy(
                    hits = existing.hits + 1,
                    lastSeenAt = seenAt,
                    lastTill = till ?: existing.lastTill,
                ) ?: UnknownBarcodeEntity(
                    code = code,
                    hits = 1,
                    firstSeenAt = seenAt,
                    lastSeenAt = seenAt,
                    lastTill = till,
                )
            )
        }
    }

    override fun barcodesFor(productId: String): Flow<List<ProductBarcode>> =
        productBarcodeDao.getByProductId(productId).map { list -> list.map { it.toDomain() } }

    override suspend fun addBarcode(productId: String, rawCode: String): Boolean {
        val code = BarcodeNormaliser.normalise(rawCode) ?: return false
        val existing = productBarcodeDao.findByCode(code)
        if (existing != null) return existing.productId == productId
        productBarcodeDao.insert(
            ProductBarcodeEntity(
                id = UUID.randomUUID().toString(),
                productId = productId,
                code = code,
                createdAt = Instant.now().toEpochMilli(),
            )
        )
        return true
    }

    override suspend fun removeBarcode(rawCode: String) {
        BarcodeNormaliser.normalise(rawCode)?.let { productBarcodeDao.deleteByCode(it) }
    }

    override fun unknownBarcodes(): Flow<List<UnknownBarcode>> =
        unknownBarcodeDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun unknownBarcodeCount(): Flow<Int> = unknownBarcodeDao.countAll()

    override suspend fun claimUnknown(rawCode: String, productId: String) {
        val code = BarcodeNormaliser.normalise(rawCode) ?: return
        db.withTransaction {
            addBarcode(productId, code)
            unknownBarcodeDao.deleteByCode(code)
        }
    }
}
