package net.kuama.wifiSpy.implementation

import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import net.kuama.wifiSpy.WiFiListener

/**
 * From Android Q on, most of the WiFi-related classes and functionalities have been deprecated
 * https://developer.android.com/about/versions/10/behavior-changes-10
 *
 * From now on, to observe connectivity changes we should register a
 * [ConnectivityManager.NetworkCallback] implementation
 */
@TargetApi(Build.VERSION_CODES.N)
internal class AndroidQ(context: Context) : WiFiListener {

    /**
     * Callback to propagate the "wifi state changed" action
     */
    var onChange: (() -> Unit)? = null

    /**
     * Registers the network callback
     */
    private val startImplementation = {
        (context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .registerDefaultNetworkCallback(networkCallback)
    }

    /**
     * Unregisters the network callback
     */
    private val stopImplementation = {
        (context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .unregisterNetworkCallback(networkCallback)
    }

    /**
     * Simple [ConnectivityManager.NetworkCallback] implementation
     * will invoke the onChange on each onCapabilitiesChanged
     */
    private val networkCallback = object :
        ConnectivityManager.NetworkCallback() {

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            onChange?.invoke()
        }
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