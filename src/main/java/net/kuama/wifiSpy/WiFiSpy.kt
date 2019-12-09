package net.kuama.wifiSpy

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
import net.kuama.wifiSpy.data.WiFiInfo
import net.kuama.wifiSpy.data.WiFiState
import net.kuama.wifiSpy.implementation.AndroidQ
import net.kuama.wifiSpy.implementation.BeforeAndroidQ
import net.kuama.wifiSpy.support.HotObservable
import net.kuama.wifiSpy.support.SingletonHolder

class WiFiSpy private constructor(context: Context) {

    private var async: Deferred<Unit>? = null

    private var wiFiListener: WiFiListener =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AndroidQ(context)
        } else {
            // on android < 10 we need to take a totally different approach
            BeforeAndroidQ(context)
        }

    /**
     *
     */
    private var wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


    private val isFineLocationAccessGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PERMISSION_GRANTED

    /**
     *
     */
    companion object : SingletonHolder<WiFiSpy, Context>(::WiFiSpy)

    /**
     *
     */
    private var subscribersCount = 0

    /**
     *
     */
    private var innerInfo: HotObservable<WiFiInfo>? = null

    /**
     * Upon subscription will start listening for wifi changes
     */
    val info: Observable<WiFiInfo> by lazy {
        innerInfo =
            HotObservable(WiFiInfo(WiFiState.UNKNOWN))
        innerInfo!!.observe().doOnSubscribe {

            if (subscribersCount == 0) {
                async = GlobalScope.async {
                    // start listening for wi-fi state changes
                    listenWiFiChanges()
                }
            }

            // keep track of subscribers count
            subscribersCount += 1

            println("we got $subscribersCount subscribers")
        }
            .doOnError {
                println("error " + it.localizedMessage)
                it.printStackTrace()
            }
            .doOnDispose {
                subscribersCount -= 1
                if (subscribersCount == 0) {
                    stopListeningWiFiChanges()
                    async?.cancel()
                }
            }
    }


    /**
     * Will check the current wifi state and propagate different infos
     * based on it.
     */
    private fun checkWiFi() {

        when (wifiManager.wifiState) {
            WifiManager.WIFI_STATE_DISABLED, WifiManager.WIFI_STATE_DISABLING -> onWiFiDisabled()
            WifiManager.WIFI_STATE_ENABLED -> onWiFiEnabled(wifiManager.connectionInfo)
            WifiManager.WIFI_STATE_ENABLING -> noop()
            else -> onCouldNotGetWiFiState()
        }
    }

    /**
     * Propagates a [WiFiState.DISCONNECTED] state
     */
    private fun onWiFiDisabled() {
        innerInfo?.current =
            WiFiInfo(WiFiState.DISCONNECTED)
    }

    /**
     * Propagates a [WiFiState.CONNECTED] state, and the
     * current wi-fi ssid (if android.permission.ACCESS_FINE_LOCATION was granted,
     * <unknown-ssid> otherwise)
     */
    private fun onWiFiEnabled(connectionInfo: WifiInfo) {
        innerInfo?.current = WiFiInfo(
            if (isFineLocationAccessGranted) WiFiState.CONNECTED else WiFiState.CONNECTED_MISSING_FINE_LOCATION_PERMISSION,
            connectionInfo.ssid
        )
    }

    /**
     * Propagates a [WiFiState.UNKNOWN] state.
     * Typically it's the first value that gets emitted to the
     * [info] subscribers
     */
    private fun onCouldNotGetWiFiState() {
        innerInfo?.current = WiFiInfo(WiFiState.UNKNOWN)
    }

    private fun noop() {}

    private fun listenWiFiChanges() {
        wiFiListener.start { checkWiFi() }
    }

    private fun stopListeningWiFiChanges() {
        wiFiListener.stop()
    }
}