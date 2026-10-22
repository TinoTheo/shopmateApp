package com.shopmate

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.shopmate.domain.repository.SettingsRepository
import com.shopmate.sync.NetworkMonitor
import com.shopmate.sync.SessionSyncTrigger
import com.shopmate.sync.SyncScheduler
import com.shopmate.ui.util.CurrencyFormatter
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltAndroidApp
class ShopMateApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var networkMonitor: NetworkMonitor
    @Inject lateinit var sessionSyncTrigger: SessionSyncTrigger
    @Inject lateinit var settingsRepository: SettingsRepository

    // Main.immediate so snapshot writes to CurrencyFormatter.symbol happen on the UI thread.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Hilt-aware WorkManager configuration (default initializer is disabled in the manifest).
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        syncScheduler.schedulePeriodicSync()
        networkMonitor.register()
        sessionSyncTrigger.start(appScope)

        // Keep the global currency symbol in sync with the merchant's choice.
        settingsRepository.currencySymbol
            .onEach { CurrencyFormatter.symbol = it }
            .launchIn(appScope)
    }
}
