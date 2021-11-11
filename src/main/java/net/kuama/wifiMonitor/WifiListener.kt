package net.kuama.wifiMonitor

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Interface for a platform-specific WiFi monitoring implementation.
 */
interface WifiListener {
    fun listen(context: Context): Flow<Unit>
}
