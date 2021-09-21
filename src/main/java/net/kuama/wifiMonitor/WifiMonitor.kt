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

/**
 * This class allows to check if any permission from the manifest has been granted
 */
class PermissionChecker(private val context: Context) {
    class Builder {
        private var context: Context? = null
        fun context(context: Context) =
            apply {
                this.context = context
            }

        fun build(): PermissionChecker {
            val context = checkNotNull(context) { "Please provide a valid context" }
            return PermissionChecker(context)
        }
    }

    /**
     * Passing a permission, it checks if the user granted.
     * The permission must be in Manifest.permission.PERMISSION_NAME form
     */
    fun check(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PERMISSION_GRANTED
    }
}

class WifiMonitor private constructor(
    private val listener: WifiListener,
    private val wifiManager: WifiManager,
    permissionChecker: PermissionChecker
) {

    /**
     * In order to be able to build a WifiMonitor instance, it is necessary to pass either:
     * - a valid context
     * or
     * - a WifiListener, WifiManager and a PermissionChecker
     */
    class WifiMonitorBuilder {

        private var listener: WifiListener? = null
        fun listener(listener: WifiListener) =
            apply {
                this.listener = listener
            }

        private fun listenerBuilder(context: Context): WifiListener {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AndroidQWifiListener(context)
            } else {
                // on android < 10 we need to take a totally different approach
                BeforeAndroidQWifiListener(context)
            }
        }

        private var wifiManager: WifiManager? = null
        fun wifiManager(wifiManager: WifiManager) = apply {
            this.wifiManager = wifiManager
        }

        private fun wifiManagerBuilder(context: Context): WifiManager {
            return context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
        }

        private var permissionChecker: PermissionChecker? = null
        fun permissionChecker(permissionChecker: PermissionChecker) = apply {
            this.permissionChecker = permissionChecker
        }

        private fun permissionCheckerBuilder(context: Context): PermissionChecker {
            return PermissionChecker.Builder()
                .context(context)
                .build()
        }

        private var context: Context? = null
        fun context(context: Context) =
            apply {
                this.context = context
            }

        fun build(): WifiMonitor {
            if (context == null) {
                // If we don't have a context, make sure we have all params to build a valid wifi monitor
                val listener = checkNotNull(listener) { "Please provide a valid wi-fi listener" }
                val wifiManager =
                    checkNotNull(wifiManager) { "Please provide a valid wi-fi manager" }
                val permissionChecker =
                    checkNotNull(permissionChecker) { "Please provide a valid permission checker" }
                return WifiMonitor(
                    listener,
                    wifiManager,
                    permissionChecker
                )
            } else {
                // If we have a context, build all the missing params in order to have a wifi monitor
                val context = checkNotNull(context) { "Please provide a valid context" }
                val listener = if (listener == null) {
                    listenerBuilder(context)
                } else {
                    this.listener!!
                }
                val wifiManager = if (wifiManager == null) {
                    wifiManagerBuilder(context)
                } else {
                    this.wifiManager!!
                }
                val permissionChecker = if (permissionChecker == null) {
                    permissionCheckerBuilder(context)
                } else {
                    this.permissionChecker!!
                }
                return WifiMonitor(
                    listener,
                    wifiManager,
                    permissionChecker
                )
            }
        }
    }

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
    private val isFineLocationAccessGranted: Boolean =
        permissionChecker.check(Manifest.permission.ACCESS_FINE_LOCATION)

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
