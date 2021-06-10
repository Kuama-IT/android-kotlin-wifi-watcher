package net.kuama.wifiMonitor

/**
 * Allows to trigger a callback whenever Wi-Fi changes its status
 */
interface WifiListener {
    fun stop()

    fun start(onChange: () -> Unit)
}
