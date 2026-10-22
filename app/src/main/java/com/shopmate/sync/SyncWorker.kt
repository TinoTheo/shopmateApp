package com.shopmate.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.StoreContext
import com.shopmate.data.local.dao.CategoryDao
import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.local.dao.SaleDao
import com.shopmate.data.local.dao.StockDeltaDao
import com.shopmate.data.local.entity.StockDeltaEntity
import com.shopmate.data.mapper.toDomain
import com.shopmate.data.remote.ShopMateRemote
import com.shopmate.data.remote.dto.SnapshotPayload
import com.shopmate.data.remote.dto.StoreSnapshotRowDto
import com.shopmate.data.remote.mapper.toServerProductDto
import com.shopmate.data.remote.mapper.toSnapshotSbDto
import com.shopmate.domain.model.DeltaReason
import com.shopmate.domain.model.EventType
import com.shopmate.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Periodic background sync (every 15 min via WorkManager, its periodic minimum). Pushes pending
 * sales, unsynced stock deltas, and a store snapshot through [ShopMateRemote], then pulls server
 * product metadata. Signed out (or no active cloud store) → no-op success, offline-friendly.
 *
 * A `401` at any step stops the drain: nothing is marked failed, the session is flagged for
 * re-authentication, and everything stays held on the device until the cashier signs in again.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val remote: ShopMateRemote,
    private val syncRepository: SyncRepository,
    private val saleDao: SaleDao,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val stockDeltaDao: StockDeltaDao,
    private val storeContext: StoreContext,
    private val authManager: AuthManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!authManager.hasUsableSession()) return Result.success()
        val storeId = storeContext.getActiveStoreId() ?: return Result.success()
        if (!isNetworkAvailable()) return Result.retry()

        return try {
            syncPendingSales(storeId)
            syncStockDeltas(storeId)
            syncSnapshot(storeId)
            pullServerUpdates(storeId)
            Result.success()
        } catch (e: SessionRejected) {
            authManager.markSessionRejected()
            Result.success() // retrying with the same session would only be refused again
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun syncPendingSales(storeId: String) {
        val pendingSaleEvents = syncRepository.getPendingEvents().first()
            .filter { it.eventType == EventType.SALE }
        if (pendingSaleEvents.isEmpty()) return

        // Payload IS the sale id (queued by SaleRepository.recordSale).
        val saleByEvent = pendingSaleEvents.associateWith { saleDao.getById(it.payload)?.toDomain() }
        val sales = saleByEvent.values.filterNotNull()

        if (sales.isEmpty()) {
            // Sales no longer exist locally — retire their events so the queue can't stall.
            pendingSaleEvents.forEach { syncRepository.markSynced(it.id) }
            return
        }

        try {
            remote.upsertSales(sales, storeId)
        } catch (e: RestException) {
            if (e.statusCode == 401) throw SessionRejected()
            if (e.statusCode in 400..499) {
                pendingSaleEvents.forEach {
                    syncRepository.markFailed(it.id, "Supabase ${e.statusCode}: ${e.error}")
                }
                return
            }
            throw e
        }

        val now = Instant.now().toEpochMilli()
        saleByEvent.forEach { (event, sale) ->
            syncRepository.markSynced(event.id)
            sale?.let { saleDao.markSynced(it.id, now) }
        }
    }

    /**
     * Sale movements and every other movement go up as separate requests. RLS lets any member
     * insert SALE movements but only owners and managers insert the rest, and one INSERT is
     * all-or-nothing — in a single batch, one restock by a cashier would hold back every sale
     * movement with it, on every run.
     */
    private suspend fun syncStockDeltas(storeId: String) {
        val (saleMovements, otherMovements) = stockDeltaDao.getUnsynced()
            .partition { it.reason == DeltaReason.SALE }
        uploadStockDeltas(saleMovements, storeId)
        uploadStockDeltas(otherMovements, storeId)
    }

    private suspend fun uploadStockDeltas(batch: List<StockDeltaEntity>, storeId: String) {
        if (batch.isEmpty()) return
        try {
            remote.upsertStockDeltas(batch, storeId)
        } catch (e: RestException) {
            if (e.statusCode == 401) throw SessionRejected()
            if (e.statusCode in 400..499) return // schema/RLS rejection — stays on the device
            throw e
        }
        stockDeltaDao.markSynced(batch.map { it.id })
    }

    private suspend fun syncSnapshot(storeId: String) {
        // Capture the events this snapshot covers BEFORE reading data — an adjustment made
        // during the upload queues a new event that must survive for the next run.
        val coveredEvents = syncRepository.getPendingEvents().first()
            .filter { it.eventType == EventType.SNAPSHOT }

        val products = productDao.getAll().first()
        val categories = categoryDao.getAll().first()
        val categoryNames = categories.associate { it.id to it.name }

        val row = StoreSnapshotRowDto(
            storeId = storeId,
            takenAt = Instant.now().toString(),
            payload = SnapshotPayload(
                products = products.map { product ->
                    product.toDomain().toSnapshotSbDto(categoryName = product.categoryId?.let { categoryNames[it] })
                },
                categories = categories.map { it.toDomain().toSnapshotSbDto() },
            ),
        )

        try {
            remote.insertSnapshot(row)
        } catch (e: RestException) {
            if (e.statusCode == 401) throw SessionRejected()
            if (e.statusCode in 400..499) {
                coveredEvents.forEach {
                    syncRepository.markFailed(it.id, "Supabase ${e.statusCode}: ${e.error}")
                }
                return
            }
            throw e
        }
        coveredEvents.forEach { syncRepository.markSynced(it.id) }
    }

    private suspend fun pullServerUpdates(storeId: String) {
        val rows = try {
            remote.fetchProducts(storeId)
        } catch (e: RestException) {
            return // pull is best-effort; push already succeeded
        }
        rows.forEach { ConflictResolver.resolveProduct(it.toServerProductDto(), storeId, productDao) }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** The server refused the session (`401`). Unwinds the whole drain, not just one step. */
    private class SessionRejected : Exception()
}
