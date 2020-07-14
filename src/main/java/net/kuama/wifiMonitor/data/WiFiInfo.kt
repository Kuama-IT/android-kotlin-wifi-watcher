package net.kuama.wifiMonitor.data

/**
 * Simple object to pass over spied info about wifi through the wifispy info flowable
 */
class WiFiInfo(
    /**
     * connection state
     */
    val state: WifiState,
    /**
     * the name of the current access point.
     */
    val ssid: String? = null,
    /**
     * the basic service set identifier (BSSID) of the current access point.
     */
    val bssid: String? = null,
    /**
     * 2.4 or 5 ghz (or unknown) of the current access point.
     */
    val band: WifiNetworkBand = WifiNetworkBand.UNKNOWN,
    /**
     * the received signal strength indicator of the current 802.11
     * network, in dBm.
     */
    val rssi: Int? = null
)
