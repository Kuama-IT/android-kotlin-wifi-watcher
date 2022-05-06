package net.kuama.wifiMonitor.implementation

import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.os.Build
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.kuama.wifiMonitor.WifiListener

/**
 * WiFi Listener for Android SDK 29 (Android 10) and above.
 *
 * From Android Q on, most of the Wi-Fi-related classes and properties have been deprecated.
 * https://developer.android.com/about/versions/10/behavior-changes-10
 *
 * From now on, to observe connectivity changes we should register a [ConnectivityManager.NetworkCallback] implementation.
 */
@TargetApi(Build.VERSION_CODES.Q)
internal class AndroidQWifiListener : WifiListener {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun listen(context: Context): Flow<WifiInfo?> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)

                val wifiInfo = networkCapabilities.transportInfo as WifiInfo
                @Suppress("BlockingMethodInNonBlockingContext")
                trySendBlocking(wifiInfo)
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Register the callback on network changes.
        connectivityManager.registerNetworkCallback(request, callback)

        // Wait for flow cancellation, then unregister the callback.
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
