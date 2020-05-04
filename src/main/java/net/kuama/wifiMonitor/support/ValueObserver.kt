package net.kuama.wifiSpy.support

interface ValueObserver<T> {
    fun valueChanged(anyValue: T)
}