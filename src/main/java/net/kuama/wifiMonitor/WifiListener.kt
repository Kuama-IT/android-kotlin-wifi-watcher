package net.kuama.wifiSpy

internal interface WifiListener {
    fun stop()

    fun start(onChange: () -> Unit)
}