package net.kuama.wifiMonitor

internal interface WifiListener {
    fun stop()

    fun start(onChange: () -> Unit)
}