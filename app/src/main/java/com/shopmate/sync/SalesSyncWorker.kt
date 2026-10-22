package com.shopmate.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.StoreContext
import com.shopmate.data.local.dao.SaleDao
import com.shopmate.data.mapper.toDomain
import com.shopmate.data.remote.ShopMateRemote
import com.shopmate.domain.model.EventType
import com.shopmate.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Fire-and-forget upload of a single sale, enqueued immediately after checkout. Best-effort:
 * any failure is swallowed as success so WorkManager doesn't retry — the periodic [SyncWorker]
 * is the safety net that eventually flushes anything left pending.
 */
@HiltWorker
class SalesSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val remote: ShopMateRemote,
    private val saleDao: SaleDao,
    private val syncRepository: SyncRepository,
    private val storeContext: StoreContext,
    private val authManager: AuthManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!authManager.hasUsableSession()) return Result.success()
        val saleId = inputData.getString(KEY_SALE_ID) ?: return Result.failure()
        val storeId = storeContext.getActiveStoreId() ?: return Result.success()

        return try {
            val sale = saleDao.getById(saleId)?.toDomain() ?: return Result.success()
            remote.upsertSales(listOf(sale), storeId)
            saleDao.markSynced(saleId, Instant.now().toEpochMilli())
            syncRepository.getPendingEvents().first()
                .filter { it.eventType == EventType.SALE && it.payload == saleId }
                .forEach { syncRepository.markSynced(it.id) }
            Result.success()
        } catch (e: RestException) {
            if (e.statusCode == 401) authManager.markSessionRejected()
            Result.success() // one-shot: the sale stays held for the periodic worker
        } catch (e: Exception) {
            Result.success() // one-shot: leave it for the periodic worker
        }
    }

    companion object {
        const val KEY_SALE_ID = "sale_id"
    }
}
