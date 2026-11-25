package com.hefimer.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Observes real-time validated internet connectivity via [ConnectivityManager.NetworkCallback].
 * Tracks the active set of validated internet networks without calling synchronous activeNetwork
 * queries inside callbacks (avoiding stale OS binder state during rapid Wi-Fi/Cellular switches).
 */
class NetworkMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val validatedNetworks = ConcurrentHashMap.newKeySet<Network>()
    private val _isOnline = MutableStateFlow(seedInitialState())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val caps = runCatching { connectivityManager?.getNetworkCapabilities(network) }.getOrNull()
            if (isUsable(caps)) {
                validatedNetworks.add(network)
                _isOnline.value = true
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            if (isUsable(networkCapabilities)) {
                validatedNetworks.add(network)
            } else {
                validatedNetworks.remove(network)
            }
            _isOnline.value = validatedNetworks.isNotEmpty()
        }

        override fun onLost(network: Network) {
            validatedNetworks.remove(network)
            _isOnline.value = validatedNetworks.isNotEmpty()
        }

        override fun onUnavailable() {
            validatedNetworks.clear()
            _isOnline.value = false
        }
    }

    init {
        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, callback)
        }
    }

    fun isConnectedNow(): Boolean {
        val cm = connectivityManager ?: return true
        val net = cm.activeNetwork
        val caps = net?.let { runCatching { cm.getNetworkCapabilities(it) }.getOrNull() }
        val usableNow = isUsable(caps) || validatedNetworks.isNotEmpty()
        _isOnline.value = usableNow
        return usableNow
    }

    private fun seedInitialState(): Boolean {
        val cm = connectivityManager ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = runCatching { cm.getNetworkCapabilities(net) }.getOrNull()
        val ok = isUsable(caps)
        if (ok) validatedNetworks.add(net)
        return ok
    }

    private fun isUsable(caps: NetworkCapabilities?): Boolean {
        if (caps == null) return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun close() {
        runCatching {
            connectivityManager?.unregisterNetworkCallback(callback)
        }
        validatedNetworks.clear()
    }
}
