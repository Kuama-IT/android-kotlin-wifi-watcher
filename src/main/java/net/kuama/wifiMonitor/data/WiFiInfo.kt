package net.kuama.wifiSpy.data

/**
 * Simple object to pass over spied info about wifi through the wifispy info flowable
 */
class WiFiInfo(val state: WifiState, val ssid: String? = null, val bssid: String? = null, val band: WifiNetworkBand = WifiNetworkBand.UNKNOWN)