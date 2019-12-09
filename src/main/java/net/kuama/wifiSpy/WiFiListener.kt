package net.kuama.wifiSpy

interface WiFiListener {
    fun stop()

    fun start(onChange: () -> Unit)
}