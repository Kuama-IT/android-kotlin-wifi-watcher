package net.kuama.wifiMonitor.implementation

import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
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
internal class AndroidQWifiListener(private val wifiManager: WifiManager) : WifiListener {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun listen(context: Context): Flow<WifiInfo?> = callbackFlow {
        // Android 12 requires a new flag to receive SSID data, but flag-based constructor has been introduced only in SDK 31.
        val networkCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)

                    trySendBlocking(networkCapabilities.transportInfo as? WifiInfo?)
                }
            } else
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)

                    // There's a weird behavior in SDK 30 where transportInfo is null even when connected to the WiFi.
                    // Fallback to soon-to-deprecate connectionInfo if we're connected to a WiFi network.
                    @Suppress("DEPRECATION")
                    val wifiInfo =
                        (networkCapabilities.transportInfo as? WifiInfo?)
                            ?: if (wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED) wifiManager.connectionInfo else null

                    trySendBlocking(wifiInfo)
                }
            }

        val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Register the callback on network changes.
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        // Wait for flow cancellation, then unregister the callback.
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}
