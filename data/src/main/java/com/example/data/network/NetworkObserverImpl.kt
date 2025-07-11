package com.example.data.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.domain.network.NetworkObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject

class NetworkObserverImpl @Inject constructor(
    private val connectivityManager: ConnectivityManager
) : NetworkObserver {

    override val isNetworkAvailable: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(checkInternetAvailable()).isSuccess
            }

            override fun onLost(network: Network) {
                trySend(checkInternetAvailable()).isSuccess
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(checkInternetAvailable()).isSuccess
            }
        }

        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, callback)

        trySend(checkInternetAvailable()).isSuccess

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
        .debounce(500)

    override fun isCurrentlyAvailable(): Boolean {
        return checkInternetAvailable()
    }

    private fun checkInternetAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
