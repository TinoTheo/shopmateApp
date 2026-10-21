package com.shopmate.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shopmate.data.auth.StoreContext
import com.shopmate.data.local.dao.CategoryDao
import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.local.dao.SaleDao
import com.shopmate.data.local.dao.StockDeltaDao
import com.shopmate.data.mapper.toDomain
import com.shopmate.data.remote.AuthTokenProvider
import com.shopmate.data.remote.dto.SnapshotPayload
import com.shopmate.data.remote.dto.StoreSnapshotRowDto
import com.shopmate.data.remote.dto.SupabaseProductDto
import com.shopmate.data.remote.mapper.toRow
import com.shopmate.data.remote.mapper.toSaleItemRows
import com.shopmate.data.remote.mapper.toSaleRow
import com.shopmate.data.remote.mapper.toServerProductDto
import com.shopmate.data.remote.mapper.toSnapshotSbDto
import com.shopmate.domain.model.EventType
import com.shopmate.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Periodic background sync (every 15 min via WorkManager, its periodic minimum). Pushes pending
 * sales, unsynced stock deltas, and a store snapshot to Supabase postgrest, then pulls server
 * product metadata. Signed out (or no active cloud store) → no-op success, offline-friendly.
 *
 * Uploads use upsert-with-ignoreDuplicates: still INSERT-only against RLS (ON CONFLICT DO
 * NOTHING — never update/delete), but safe to retry after a half-finished run.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val postgrest: Postgrest,
    private val syncRepository: SyncRepository,
    private val saleDao: SaleDao,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val stockDeltaDao: StockDeltaDao,
    private val storeContext: StoreContext,
    private val authTokenProvider: AuthTokenProvider,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!authTokenProvider.hasToken()) return Result.success()
        val storeId = storeContext.getActiveStoreId() ?: return Result.success()
        if (!isNetworkAvailable()) return Result.retry()

        return try {
            syncPendingSales(storeId)
            syncStockDeltas(storeId)
            syncSnapshot(storeId)
            pullServerUpdates(storeId)
            Result.success()
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
            postgrest.from("sales").upsert(sales.map { it.toSaleRow(storeId) }) {
                ignoreDuplicates = true
            }
            postgrest.from("sale_items").upsert(sales.flatMap { it.toSaleItemRows() }) {
                ignoreDuplicates = true
            }
        } catch (e: RestException) {
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

    private suspend fun syncStockDeltas(storeId: String) {
        val unsynced = stockDeltaDao.getUnsynced()
        if (unsynced.isEmpty()) return

        try {
            postgrest.from("stock_deltas").upsert(unsynced.map { it.toRow(storeId) }) {
                ignoreDuplicates = true
            }
        } catch (e: RestException) {
            if (e.statusCode in 400..499) return // schema/RLS rejection — leave flagged locally
            throw e
        }
        stockDeltaDao.markSynced(unsynced.map { it.id })
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
            postgrest.from("store_snapshots").insert(row)
        } catch (e: RestException) {
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
        // Numeric columns cast to text server-side so BigDecimal strings survive the round trip.
        val rows = try {
            postgrest.from("products")
                .select(
                    Columns.raw("id,name,selling_price::text,cost_price::text,stock_quantity,updated_at")
                ) {
                    filter { eq("store_id", storeId) }
                }
                .decodeList<SupabaseProductDto>()
        } catch (e: RestException) {
            return // pull is best-effort; push already succeeded
        }
        rows.forEach { ConflictResolver.resolveProduct(it.toServerProductDto(), productDao) }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
