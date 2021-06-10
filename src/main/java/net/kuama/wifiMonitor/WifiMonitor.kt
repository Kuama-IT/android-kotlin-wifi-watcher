package net.kuama.wifiMonitor

import android.Manifest
import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kuama.wifiMonitor.data.WifiStatus
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

    /**
     * Whether we received at least a Wi-Fi change status. When false, we cannot say we are connected
     */
    private var didReceiveChange = false

    /**
     * Stop listening to Wi-Fi changes
     */
    fun stop() = listener.stop()

    /**
     * Triggers the on change callback whenever the Wi-Fi changes its status
     */
    suspend fun observe(onChange: (WifiStatus) -> Unit) =
        withContext(Dispatchers.Default) {
            listener.start {
                didReceiveChange = true
                onChange(info)
            }
        }

    private val state: Int
        get() = wifiManager.wifiState

    private val connectionInfo: WifiInfo
        get() = wifiManager.connectionInfo

    private val band: WifiStatus.NetworkBand
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (connectionInfo.frequency > 3000) {
                WifiStatus.NetworkBand.WIFI_5_GHZ
            } else {
                WifiStatus.NetworkBand.WIFI_2_4_GHZ
            }
        } else {
            WifiStatus.NetworkBand.UNKNOWN
        }

    /**
     * Holds the current information on the Wi-Fi connection.
     */
    val info: WifiStatus
        get() = if (didReceiveChange) {
            when (state) {
                WifiManager.WIFI_STATE_DISABLED, WifiManager.WIFI_STATE_DISABLING -> WifiStatus(
                    WifiStatus.State.DISCONNECTED
                )
                WifiManager.WIFI_STATE_ENABLED -> WifiStatus(
                    state = if (isFineLocationAccessGranted) WifiStatus.State.CONNECTED else WifiStatus.State.CONNECTED_MISSING_FINE_LOCATION_PERMISSION,
                    ssid = connectionInfo.ssid,
                    bssid = connectionInfo.bssid,
                    band = band,
                    rssi = connectionInfo.rssi
                )
                WifiManager.WIFI_STATE_ENABLING -> WifiStatus(WifiStatus.State.ENABLING)
                else -> WifiStatus(WifiStatus.State.UNKNOWN)
            }
        } else {
            WifiStatus(WifiStatus.State.UNKNOWN)
        }

    /**
     * True if the user granted access to [Manifest.permission.ACCESS_FINE_LOCATION]
     */
    val isFineLocationAccessGranted: Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PERMISSION_GRANTED
}
