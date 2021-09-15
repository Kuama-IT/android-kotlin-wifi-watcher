package net.kuama.wifiMonitor

import android.Manifest
import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.kuama.wifiMonitor.data.WifiStatus
import net.kuama.wifiMonitor.data.WifiStatus.State
import net.kuama.wifiMonitor.implementation.AndroidQWifiListener
import net.kuama.wifiMonitor.implementation.BeforeAndroidQWifiListener

class WifiMonitor(context: Context) {

    private val listener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        AndroidQWifiListener(context)
    } else {
        // on android < 10 we need to take a totally different approach
        BeforeAndroidQWifiListener(context)
    }

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val state: Int
        get() = wifiManager.wifiState

    private val connectionInfo: WifiInfo
        get() = wifiManager.connectionInfo

    private val band: WifiStatus.NetworkBand
        get() =
            if (connectionInfo.frequency > 3000) {
                WifiStatus.NetworkBand.WIFI_5_GHZ
            } else {
                WifiStatus.NetworkBand.WIFI_2_4_GHZ
            }

    /**
     * True if the user granted access to [Manifest.permission.ACCESS_FINE_LOCATION]
     */
    val isFineLocationAccessGranted: Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PERMISSION_GRANTED

    /**
     * Start listening to Wi-fi changes exposing a flow of WifiStatus.
     * It can throw an exception when the channel publishing on a channel that is closed
     */
    @ExperimentalCoroutinesApi
    suspend fun start(): Flow<WifiStatus> = callbackFlow {
        var wifiStatus: WifiStatus
        listener.start {
            // Update wifiStatus value accordingly with the new state
            wifiStatus = when (state) {
                WifiManager.WIFI_STATE_DISABLED, WifiManager.WIFI_STATE_DISABLING -> WifiStatus(
                    State.DISCONNECTED
                )
                WifiManager.WIFI_STATE_ENABLED -> WifiStatus(
                    state = if (isFineLocationAccessGranted) State.CONNECTED else State.CONNECTED_MISSING_FINE_LOCATION_PERMISSION,
                    ssid = connectionInfo.ssid,
                    bssid = connectionInfo.bssid,
                    band = band,
                    rssi = connectionInfo.rssi
                )
                WifiManager.WIFI_STATE_ENABLING -> WifiStatus(State.ENABLING)
                else -> WifiStatus(State.UNKNOWN)
            }
            channel.trySend(wifiStatus)
                .isSuccess
        }
        awaitClose {
            wifiStatus = WifiStatus(State.UNKNOWN)
            stop()
        }
    }

    /**
     * Stop listening to Wi-Fi changes
     */
    fun stop() = listener.stop()
}
