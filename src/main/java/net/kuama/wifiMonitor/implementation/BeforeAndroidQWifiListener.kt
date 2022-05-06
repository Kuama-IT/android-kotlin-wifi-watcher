package net.kuama.wifiMonitor.implementation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.kuama.wifiMonitor.WifiListener

/**
 * WiFi Listener for Android SDK 28 (Android 9) and below.
 *
 * Before Android Q we can register an intent receiver to observe wifi state changes.
 */
internal class BeforeAndroidQWifiListener(private val wifiManager: WifiManager) : WifiListener {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun listen(context: Context): Flow<WifiInfo?> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                @Suppress("DEPRECATION")
                trySendBlocking(if (wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED) wifiManager.connectionInfo else null)
            }
        }

        // Register the intent receiver for network state change.
        val filter = IntentFilter()
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED")
        filter.addAction("android.net.wifi.STATE_CHANGE")
        context.registerReceiver(receiver, filter)

        // Wait for flow cancellation, then unregister the receiver.
        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }
}
