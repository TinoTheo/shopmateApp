package com.shopmate.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Single entry point for triggering sync work — periodic, per-sale, and manual. */
@Singleton
class SyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun syncSaleImmediately(saleId: String) {
        val request = OneTimeWorkRequestBuilder<SalesSyncWorker>()
            .setConstraints(networkConstraint)
            .setInputData(workDataOf(SalesSyncWorker.KEY_SALE_ID to saleId))
            .build()
        workManager.enqueue(request)
    }

    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraint)
            .build()
        workManager.enqueueUniqueWork(MANUAL_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(PERIODIC_WORK)
    }

    companion object {
        const val PERIODIC_WORK = "shopmate_periodic_sync"
        const val MANUAL_WORK = "shopmate_manual_sync"
    }
}
