package com.shopmate.sync

import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.StoreContext
import com.shopmate.domain.repository.StorePermissionRepository
import com.shopmate.domain.repository.DeviceRepository
import com.shopmate.domain.repository.LicenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acts the moment the till can talk to the server for a store again — no "Sync now" needed.
 *
 * [NetworkMonitor] covers the network coming back. This covers the session and the store: a
 * sign-in, a sign-in after the server rejected the old session, a store being picked or switched,
 * or opening the app signed in. Each time, it:
 * - enqueues a sync, which carries a network constraint, so offline it waits for
 *   [NetworkMonitor]'s reconnect;
 * - asks the server again what this member may do at the store ([StorePermissionRepository]).
 */
@Singleton
class SessionSyncTrigger @Inject constructor(
    private val authManager: AuthManager,
    private val storeContext: StoreContext,
    private val syncScheduler: SyncScheduler,
    private val storePermissions: StorePermissionRepository,
    private val licences: LicenceRepository,
    private val deviceRepository: DeviceRepository,
) {
    fun start(scope: CoroutineScope) {
        // Becoming a till is also a moment to sync: its first registration and pull start at once.
        combine(authManager.needsReauth, storeContext.activeStoreIdFlow, deviceRepository.role) { needsReauth, storeId, role ->
            storeId.takeUnless { needsReauth }?.let { it to role }
        }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach {
                syncScheduler.syncNow()
                storePermissions.refresh()
                licences.refresh()
            }
            .launchIn(scope)
    }
}
