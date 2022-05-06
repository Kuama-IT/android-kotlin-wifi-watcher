package net.kuama.wifiMonitor

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.kuama.wifiMonitor.data.WifiStatus
import net.kuama.wifiMonitor.data.WifiStatus.State
import net.kuama.wifiMonitor.implementation.AndroidQWifiListener
import net.kuama.wifiMonitor.implementation.BeforeAndroidQWifiListener

class WifiMonitor private constructor(
    private val context: Context,
    private val listener: WifiListener,
    private val wifiManager: WifiManager,
    permissionChecker: PermissionChecker
) {
    class Builder {
        private var listener: WifiListener? = null
        fun listener(listener: WifiListener) = apply { this.listener = listener }

        private fun listenerBuilder(): WifiListener =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AndroidQWifiListener()
            } else {
                BeforeAndroidQWifiListener()
            }

        private var wifiManager: WifiManager? = null
        fun wifiManager(wifiManager: WifiManager) = apply { this.wifiManager = wifiManager }

        private fun wifiManagerBuilder(context: Context): WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        private var permissionChecker: PermissionChecker? = null
        fun permissionChecker(permissionChecker: PermissionChecker) = apply { this.permissionChecker = permissionChecker }

        private fun permissionCheckerBuilder(context: Context): PermissionChecker = PermissionChecker.Builder().context(context).build()

        fun build(context: Context): WifiMonitor = WifiMonitor(
            context = context,
            listener = listener ?: listenerBuilder(),
            wifiManager = wifiManager ?: wifiManagerBuilder(context),
            permissionChecker = permissionChecker ?: permissionCheckerBuilder(context),
        )
    }

    /**
     * Whether the user granted access to [Manifest.permission.ACCESS_FINE_LOCATION].
     */
    private val isFineLocationAccessGranted: Boolean = permissionChecker.check(Manifest.permission.ACCESS_FINE_LOCATION)

    /**
     * Monitor WiFi status and changes.
     *
     * @return A flow of WiFi statuses.
     */
    fun monitor(): Flow<WifiStatus> = listener.listen(context).map { wifiInfo ->
        when (wifiManager.wifiState) {
            WifiManager.WIFI_STATE_DISABLED, WifiManager.WIFI_STATE_DISABLING -> WifiStatus(
                State.DISCONNECTED
            )
            WifiManager.WIFI_STATE_ENABLED -> {
                val connectionInfo = wifiInfo ?: @Suppress("DEPRECATION") wifiManager.connectionInfo
                WifiStatus(
                    state = if (isFineLocationAccessGranted) State.CONNECTED else State.CONNECTED_MISSING_FINE_LOCATION_PERMISSION,
                    ssid = connectionInfo.ssid,
                    bssid = connectionInfo.bssid,
                    band = if (connectionInfo.frequency > 3000) WifiStatus.NetworkBand.WIFI_5_GHZ else WifiStatus.NetworkBand.WIFI_2_4_GHZ,
                    rssi = connectionInfo.rssi
                )
            }
            WifiManager.WIFI_STATE_ENABLING -> WifiStatus(State.ENABLING)
            else -> WifiStatus(State.UNKNOWN)
        }
    }
}
