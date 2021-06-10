package net.kuama.wifiMonitor.data

import android.Manifest
import net.kuama.wifiMonitor.WifiMonitor

/**
 * Collects a set of useful properties about the WiFi
 */
class WifiStatus(
    /**
     * Connection state
     */
    val state: State,

    /**
     * Name of the current access point.
     * Can be "<unknown-ssid>" when [Manifest.permission.ACCESS_FINE_LOCATION] has not been granted
     */
    val ssid: String? = null,

    /**
     * Basic service set identifier (BSSID) of the current access point.
     */
    val bssid: String? = null,

    /**
     * 2.4 or 5 ghz (or unknown) of the current access point.
     */
    val band: NetworkBand = NetworkBand.UNKNOWN,

    /**
     * Signal strength indicator of the current 802.11 network, in dBm.
     */
    val rssi: Int? = null
) {
    enum class State {
        /**
         * Wi-Fi is currently connected and we should be able to read all of the information we need
         */
        CONNECTED,

        /**
         * If Wi-Fi is connected but you didn't ask your user for fine location access permission
         * you'll get this state with the "<unknown-ssid>" string inside a [WifiStatus] object
         */
        CONNECTED_MISSING_FINE_LOCATION_PERMISSION,

        /**
         * Wi-Fi is currently being disabled or Wi-Fi is disabled
         */
        DISCONNECTED,

        /**
         * Wi-Fi is currently being enabled. The state will change to {@link #WIFI_STATE_ENABLED} if
         * it finishes successfully.
         */
        ENABLING,

        /**
         * Either there was an error trying to read Wi-Fi state or nobody called [WifiMonitor.observe]
         */
        UNKNOWN
    }

    enum class NetworkBand {
        UNKNOWN, WIFI_2_4_GHZ, WIFI_5_GHZ
    }
}
