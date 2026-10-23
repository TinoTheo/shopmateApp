package com.shopmate.data.repository

import com.shopmate.data.auth.StoreContext
import com.shopmate.data.local.AppDatabase
import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.local.dao.StockDeltaDao
import com.shopmate.data.local.dao.SyncEventDao
import com.shopmate.data.remote.tenancyRefusal
import com.shopmate.domain.repository.SettingsRepository
import com.shopmate.domain.repository.StoreChoice
import com.shopmate.domain.repository.StoreRepository
import com.shopmate.domain.repository.StoreSwitch
import com.shopmate.domain.repository.TenancyResult
import com.shopmate.sync.NetworkMonitor
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val postgrest: Postgrest,
    private val storeContext: StoreContext,
    private val productDao: ProductDao,
    private val syncEventDao: SyncEventDao,
    private val stockDeltaDao: StockDeltaDao,
    private val settings: SettingsRepository,
    private val networkMonitor: NetworkMonitor,
) : StoreRepository {

    override suspend fun accessibleStores(): List<StoreChoice> =
        storeContext.fetchAccessibleStores().map { StoreChoice(it.id, it.name) }

    override suspend fun switchTo(store: StoreChoice): StoreSwitch = moveTill(store)

    override suspend fun resolveOnSignIn(): StoreSwitch? {
        val stores = runCatching { accessibleStores() }.getOrDefault(emptyList())
        val home = storeContext.localDataStoreId()
        val target = stores.firstOrNull { it.id == home } ?: stores.firstOrNull() ?: return null
        return moveTill(target)
    }

    override suspend fun createOrganization(
        orgName: String,
        storeName: String,
        currencySymbol: String,
    ): TenancyResult<StoreSwitch> {
        val storeId = runCatching {
            storeContext.createOrganizationWithStore(orgName.trim(), storeName.trim(), currencySymbol)
        }.getOrElse { return TenancyResult.Refused(it.tenancyRefusal()) }
        return TenancyResult.Done(moveTill(StoreChoice(storeId, storeName.trim())))
    }

    override suspend fun createStore(name: String, currencySymbol: String): TenancyResult<StoreChoice> =
        runCatching {
            postgrest.rpc(
                function = "create_store",
                parameters = buildJsonObject {
                    put("p_name", name.trim())
                    put("p_currency", currencySymbol)
                },
            ).decodeAs<String>()
        }.fold(
            onSuccess = { TenancyResult.Done(StoreChoice(it, name.trim())) },
            onFailure = { TenancyResult.Refused(it.tenancyRefusal()) },
        )

    override suspend fun joinWithCode(code: String): TenancyResult<StoreSwitch> {
        val storeId = runCatching {
            postgrest.rpc(
                function = "redeem_invite",
                parameters = buildJsonObject { put("p_code", code) },
            ).decodeAs<String>()
        }.getOrElse { return TenancyResult.Refused(it.tenancyRefusal()) }

        val store = runCatching { accessibleStores() }.getOrDefault(emptyList())
            .firstOrNull { it.id == storeId } ?: StoreChoice(storeId, "")
        return TenancyResult.Done(moveTill(store))
    }

    /**
     * The one way the till changes store. Its local data belongs to [StoreContext.localDataStoreId];
     * moving to that same store, or from an empty till, keeps everything. Moving anywhere else
     * clears the till — refused while anything on it is unsent, because those rows exist only here.
     * Setting the new active store starts a sync, and an empty till's first sync loads its catalogue.
     */
    private suspend fun moveTill(store: StoreChoice): StoreSwitch {
        val home = storeContext.localDataStoreId()
        val tillIsEmpty = productDao.count() == 0 && unsentCount() == 0
        if (home != store.id && home != null && !tillIsEmpty) {
            if (!networkMonitor.isOnline.value) return StoreSwitch.Offline
            val notSent = unsentCount()
            if (notSent > 0) return StoreSwitch.NotSentYet(notSent)
            // clearAllTables() runs its own transaction — must NOT be nested in withTransaction.
            db.clearAllTables()
        }
        if (store.name.isNotBlank() && home != store.id) settings.setStoreName(store.name)
        storeContext.setActiveStoreId(store.id)
        return StoreSwitch.Switched
    }

    private suspend fun unsentCount(): Int =
        syncEventDao.getPendingCount().first() +
            syncEventDao.getFailedCount().first() +
            stockDeltaDao.getUnsynced().size
}
