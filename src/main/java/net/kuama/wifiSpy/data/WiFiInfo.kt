package net.kuama.wifiSpy.data

/**
 * Simple object to pass over spied info about wifi through the wifispy info flowable
 */
class WiFiInfo(val state: WiFiState, val ssid: String? = null)