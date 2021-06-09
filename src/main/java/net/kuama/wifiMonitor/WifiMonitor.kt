package net.kuama.wifiMonitor

import android.Manifest
import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.LiveData
import kotlinx.coroutines.*
import net.kuama.wifiMonitor.data.WiFiInfo
import net.kuama.wifiMonitor.data.WifiNetworkBand
import net.kuama.wifiMonitor.data.WifiState
import net.kuama.wifiMonitor.implementation.AndroidQWifiListener
import net.kuama.wifiMonitor.implementation.BeforeAndroidQWifiListener

private fun noop() {}

class WifiMonitor(context: Context) {

    private val listener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        AndroidQWifiListener(context)
    } else {
        // on android < 10 we need to take a totally different approach
        BeforeAndroidQWifiListener(context)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun stop() = listener.stop()

    fun start(onChange: () -> Unit) = scope.launch { listener.start(onChange) }

    val state: Int
        get() = wifiManager.wifiState

    val connectionInfo: WifiInfo
        get() = wifiManager.connectionInfo

    val isFineLocationAccessGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PERMISSION_GRANTED
}

class WifiLiveData constructor(private val monitor: WifiMonitor) :
    LiveData<WiFiInfo>() {

    private var startJob: Job? = null

    override fun onActive() {
        super.onActive()
        startJob = monitor.start(::onWifiChange)
    }

    override fun onInactive() {
        super.onInactive()
        if (startJob?.isActive == true) {
            startJob?.cancel()
        }
        monitor.stop()
    }

    /**
     * Will check the current wifi state and propagate different infos
     * based on it.
     */
    private fun onWifiChange() {
        when (monitor.state) {
            WifiManager.WIFI_STATE_DISABLED, WifiManager.WIFI_STATE_DISABLING -> onWifiDisabled()
            WifiManager.WIFI_STATE_ENABLED -> onWifiEnabled(monitor.connectionInfo)
            WifiManager.WIFI_STATE_ENABLING -> noop()
            else -> onCouldNotGetWifiState()
        }
    }

    /**
     * Propagates a [WifiState.DISCONNECTED] state
     */
    private fun onWifiDisabled() = postValue(WiFiInfo(WifiState.DISCONNECTED))

    /**
     * Propagates a [WifiState.CONNECTED] state, and the
     * current wi-fi ssid (if android.permission.ACCESS_FINE_LOCATION was granted,
     * <unknown-ssid> otherwise)
     */
    private fun onWifiEnabled(connectionInfo: WifiInfo) {

        val band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (connectionInfo.frequency > 3000) {
                WifiNetworkBand.WIFI_5_GHZ
            } else {
                WifiNetworkBand.WIFI_2_4_GHZ
            }
        } else {
            WifiNetworkBand.UNKNOWN
        }

        postValue(
            WiFiInfo(
                state = if (monitor.isFineLocationAccessGranted) WifiState.CONNECTED else WifiState.CONNECTED_MISSING_FINE_LOCATION_PERMISSION,
                ssid = connectionInfo.ssid,
                bssid = connectionInfo.bssid,
                band = band,
                rssi = connectionInfo.rssi
            )
        )
    }

    /**
     * Propagates a [WifiState.UNKNOWN] state.
     * Typically it's the first value that gets emitted to the subscribers
     */
    private fun onCouldNotGetWifiState() = postValue(WiFiInfo(WifiState.UNKNOWN))
}
