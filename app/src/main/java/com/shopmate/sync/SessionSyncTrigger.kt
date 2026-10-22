package com.shopmate.sync

import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.StoreContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Flushes held sales the moment the till can upload again — no "Sync now" needed.
 *
 * [NetworkMonitor] covers the network coming back. This covers the session: a sign-in, a sign-in
 * after the server rejected the old session, or a store being picked. Each time the till goes from
 * "can't upload" to "can upload", a sync is enqueued; it carries a network constraint, so offline
 * it simply waits for [NetworkMonitor]'s reconnect.
 */
@Singleton
class SessionSyncTrigger @Inject constructor(
    private val authManager: AuthManager,
    private val storeContext: StoreContext,
    private val syncScheduler: SyncScheduler,
) {
    fun start(scope: CoroutineScope) {
        combine(authManager.needsReauth, storeContext.activeStoreIdFlow) { needsReauth, storeId ->
            !needsReauth && storeId != null
        }
            .distinctUntilChanged()
            .filter { canUpload -> canUpload }
            .onEach { syncScheduler.syncNow() }
            .launchIn(scope)
    }
}
