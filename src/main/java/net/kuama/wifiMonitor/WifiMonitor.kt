package net.kuama.wifiMonitor

import android.Manifest
import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import io.reactivex.Observable
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.kuama.wifiMonitor.data.WiFiInfo
import net.kuama.wifiMonitor.data.WifiState
import net.kuama.wifiMonitor.data.WifiNetworkBand
import net.kuama.wifiMonitor.implementation.AndroidQWifiListener
import net.kuama.wifiMonitor.implementation.BeforeAndroidQWifiListener
import net.kuama.wifiMonitor.support.HotObservable
import net.kuama.wifiMonitor.support.SingletonHolder

class WifiMonitor private constructor(context: Context) {

    class Builder {
        private var context: Context? = null
        fun context(context: Context) = apply {
            this.context = context
        }

        fun build(): WifiMonitor {
            val ctx = context ?: error("please provide a context")
            return WifiMonitor(ctx)
        }
    }

    private var listenWifiChangesAsync: Deferred<Unit>? = null

    private var wifiListener: WifiListener =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AndroidQWifiListener(context)
            } else {
                // on android < 10 we need to take a totally different approach
                BeforeAndroidQWifiListener(context)
            }

    private var wifiManager: WifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


    private val isFineLocationAccessGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
    ) == PERMISSION_GRANTED

    companion object : SingletonHolder<WifiMonitor, Context>(::WifiMonitor)

    private var subscribersCount = 0

    private var innerInfo: HotObservable<WiFiInfo>? = null

    /**
     * Upon subscription will start listening for wifi changes
     */
    val info: Observable<WiFiInfo> by lazy {
        innerInfo =
                HotObservable(WiFiInfo(WifiState.UNKNOWN))
        innerInfo!!.observe().doOnSubscribe {

            if (subscribersCount == 0) {
                listenWifiChangesAsync = GlobalScope.async {
                    // start listening for wi-fi state changes
                    listenWiFiChanges()
                }
            }

            // keep track of subscribers count
            subscribersCount += 1
        }
                .doOnError {
                    println("error " + it.localizedMessage)
                    it.printStackTrace()
                }
                .doOnDispose {
                    subscribersCount -= 1
                    if (subscribersCount == 0) {
                        stopListeningWiFiChanges()
                        listenWifiChangesAsync?.cancel()
                    }
                }
    }


    /**
     * Will check the current wifi state and propagate different infos
     * based on it.
     */
    private fun onWifiChange() {
        when (wifiManager.wifiState) {
            WifiManager.WIFI_STATE_DISABLED, WifiManager.WIFI_STATE_DISABLING -> onWifiDisabled()
            WifiManager.WIFI_STATE_ENABLED -> onWifiEnabled(wifiManager.connectionInfo)
            WifiManager.WIFI_STATE_ENABLING -> noop()
            else -> onCouldNotGetWifiState()
        }
    }

    /**
     * Propagates a [WifiState.DISCONNECTED] state
     */
    private fun onWifiDisabled() {
        innerInfo?.current =
                WiFiInfo(WifiState.DISCONNECTED)
    }

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


        innerInfo?.current = WiFiInfo(
                state = if (isFineLocationAccessGranted) WifiState.CONNECTED else WifiState.CONNECTED_MISSING_FINE_LOCATION_PERMISSION,
                ssid = connectionInfo.ssid,
                bssid = connectionInfo.bssid,
                band = band,
                rssi = connectionInfo.rssi
        )
    }

    /**
     * Propagates a [WifiState.UNKNOWN] state.
     * Typically it's the first value that gets emitted to the
     * [info] subscribers
     */
    private fun onCouldNotGetWifiState() {
        innerInfo?.current = WiFiInfo(WifiState.UNKNOWN)
    }

    private fun noop() {}

    private fun listenWiFiChanges() {
        wifiListener.start { onWifiChange() }
    }

    private fun stopListeningWiFiChanges() {
        wifiListener.stop()
    }
}