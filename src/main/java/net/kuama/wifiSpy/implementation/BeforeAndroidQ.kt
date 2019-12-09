package net.kuama.wifiSpy.implementation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import net.kuama.wifiSpy.WiFiListener

/**
 * Before Android Q we can register a receiver to obser wifi state changes,
 * and this class does just that.
 */
internal class BeforeAndroidQ(context: Context) : WiFiListener {

    /**
     * Callback to propagate the "wifi state changed" action
     */
    var onChange: (() -> Unit)? = null

    /**
     * Dummy receiver, it will just trigger the callback when the onReceive method
     * gets called
     */
    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onChange?.invoke()
        }
    }

    /**
     * unregisters the receiver
     */
    private val stopImplementation = {
        context.unregisterReceiver(receiver)
    }

    /**
     * Registers a receiver for the actions:
     * - android.net.wifi.WIFI_STATE_CHANGED
     * - android.net.wifi.STATE_CHANGE
     */
    private val startImplementation = {
        val filter = IntentFilter()
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED")
        filter.addAction("android.net.wifi.STATE_CHANGE")
        context.registerReceiver(receiver, filter)
    }

    override fun stop() {
        this.onChange = null
        stopImplementation()
    }

    override fun start(onChange: () -> Unit) {
        this.onChange = onChange
        startImplementation()
    }
}