package net.kuama.wifiMonitor.support

interface ValueObserver<T> {
    fun valueChanged(anyValue: T)
}