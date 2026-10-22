package com.shopmate.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the till can reach the internet, and since when it couldn't. Flushes pending sync the
 * moment connectivity returns. Registered for the app's lifetime.
 *
 * The Sync screen reads [isOnline] so it never shows a spinner that is really waiting for a
 * connection: offline, "Send now" is hidden and the card says the uploads will go by themselves.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
    private val syncScheduler: SyncScheduler,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val connected = mutableSetOf<Network>()

    private val _isOnline = MutableStateFlow(currentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    /** When the till last lost its connection; null while online. */
    private val _offlineSince = MutableStateFlow(if (_isOnline.value) null else Instant.now())
    val offlineSince: StateFlow<Instant?> = _offlineSince.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            synchronized(connected) { connected += network }
            update()
            syncScheduler.syncNow()
        }

        override fun onLost(network: Network) {
            synchronized(connected) { connected -= network }
            update()
        }
    }

    fun register() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
    }

    fun unregister() {
        connectivityManager.unregisterNetworkCallback(callback)
    }

    private fun update() {
        val online = synchronized(connected) { connected.isNotEmpty() }
        if (online == _isOnline.value) return
        _isOnline.value = online
        _offlineSince.value = if (online) null else Instant.now()
    }

    private fun currentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
