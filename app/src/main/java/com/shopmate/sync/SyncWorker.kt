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
import com.shopmate.domain.model.Sale
import com.shopmate.domain.model.SyncEvent
import com.shopmate.domain.repository.SaleRepository
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
    private val saleRepository: SaleRepository,
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
            val now = Instant.now().toEpochMilli()
            saleByEvent.forEach { (event, sale) ->
                syncRepository.markSynced(event.id)
                sale?.let { saleDao.markSynced(it.id, now) }
            }
            return
        } catch (e: RestException) {
            if (e.statusCode == 401) throw SessionRejected()
            if (e.statusCode !in 400..499) throw e
        }

        // The batch was refused, and one INSERT is all-or-nothing: send each sale on its own so
        // the one the server objects to can't hold back the rest.
        saleByEvent.forEach { (event, sale) ->
            if (sale == null) syncRepository.markSynced(event.id) else uploadSale(event, sale, storeId)
        }
    }

    /**
     * One sale, alone. A sale refused because its receipt number is already taken in the store
     * (issued before numbers carried a till code, e.g. by a reinstalled till) is renumbered from
     * this till and sent once more. Any other refusal marks it failed, which the header and the
     * Sync screen both show.
     */
    private suspend fun uploadSale(event: SyncEvent, sale: Sale, storeId: String) {
        var current = sale
        var renumbered = false
        while (true) {
            try {
                remote.upsertSales(listOf(current), storeId)
                syncRepository.markSynced(event.id)
                saleDao.markSynced(current.id, Instant.now().toEpochMilli())
                return
            } catch (e: RestException) {
                if (e.statusCode == 401) throw SessionRejected()
                if (e.statusCode !in 400..499) throw e
                if (!renumbered && e.isDuplicateReceiptNumber()) {
                    current = saleRepository.renumber(current.id) ?: run {
                        syncRepository.markSynced(event.id) // the sale is gone; nothing to send
                        return
                    }
                    renumbered = true
                } else {
                    syncRepository.markFailed(event.id, "Supabase ${e.statusCode}: ${e.error}")
                    return
                }
            }
        }
    }

    private fun RestException.isDuplicateReceiptNumber(): Boolean =
        statusCode == 409 && "receipt_number" in "$error $description $message"

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
