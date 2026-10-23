package com.shopmate.data.repository

import com.shopmate.data.auth.StoreContext
import com.shopmate.data.local.AppDatabase
import com.shopmate.data.local.dao.StockDeltaDao
import com.shopmate.data.local.dao.SyncEventDao
import com.shopmate.domain.repository.SettingsRepository
import com.shopmate.domain.repository.StoreChoice
import com.shopmate.domain.repository.StoreRepository
import com.shopmate.domain.repository.StoreSwitch
import com.shopmate.sync.NetworkMonitor
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val storeContext: StoreContext,
    private val syncEventDao: SyncEventDao,
    private val stockDeltaDao: StockDeltaDao,
    private val settings: SettingsRepository,
    private val networkMonitor: NetworkMonitor,
) : StoreRepository {

    override suspend fun accessibleStores(): List<StoreChoice> =
        storeContext.fetchAccessibleStores().map { StoreChoice(it.id, it.name) }

    /**
     * Refused while anything is waiting or was refused — those exist only on this till, and the
     * clear below would lose them. Once nothing is left, the old store's rows are all on the
     * server, so clearing loses nothing. Setting the new active store starts a sync, and the
     * first sync of an empty till loads the store's catalogue.
     */
    override suspend fun switchTo(store: StoreChoice): StoreSwitch {
        if (storeContext.getActiveStoreId() == store.id) return StoreSwitch.Switched
        if (!networkMonitor.isOnline.value) return StoreSwitch.Offline

        val notSent = syncEventDao.getPendingCount().first() +
            syncEventDao.getFailedCount().first() +
            stockDeltaDao.getUnsynced().size
        if (notSent > 0) return StoreSwitch.NotSentYet(notSent)

        // clearAllTables() runs its own transaction — must NOT be nested in withTransaction.
        db.clearAllTables()
        settings.setStoreName(store.name)
        storeContext.setActiveStoreId(store.id)
        return StoreSwitch.Switched
    }
}
