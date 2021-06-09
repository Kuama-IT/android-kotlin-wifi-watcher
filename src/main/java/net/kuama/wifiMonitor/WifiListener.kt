package net.kuama.wifiMonitor

interface WifiListener {
    fun stop()

    fun start(onChange: () -> Unit)
}
