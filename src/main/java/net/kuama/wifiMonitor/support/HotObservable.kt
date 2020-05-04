package net.kuama.wifiSpy.support

import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class HotObservable<T>(anyValue1: T) {

    lateinit var emitter: ValueObserver<T>

    var current: T = anyValue1
        set(value) {
            if (value != this.current) {
                field = value
                emitter.valueChanged(value)
            }
        }

    fun observe(): Observable<T> {
        return Observable.create<T> { emitter ->
            this.emitter = object : ValueObserver<T> {
                override fun valueChanged(anyValue: T) {
                    GlobalScope.launch(Dispatchers.Main) { emitter.onNext(anyValue) }
                }
            }
        }.publish().autoConnect()
    }
}