package net.kuama.wifiSpy.data

enum class WifiState {
    /**
     * if wiFi is connected
     * you'll get this state with the current wiFi's ssid inside a WiFiInfo object
     * on the flowable subscription
     */
    CONNECTED,
    /**
     * if wiFi is connected but you didn't ask your user for
     * fine location access permission
     * you'll get this state with the <unknown-ssid> string inside a WiFiInfo object
     * on the flowable subscription
     */
    CONNECTED_MISSING_FINE_LOCATION_PERMISSION,
    /**
     * if wiFi is disconnected
     * you'll get this state with a null ssid inside a WiFiInfo object
     */
    DISCONNECTED,
    /**
     * if you get this value, we could not spy on wiFi, probably you should ask your user
     * to activate the phone's WiFi
     */
    UNKNOWN
}