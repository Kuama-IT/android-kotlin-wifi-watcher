package net.kuama.wifiMonitor

import android.content.Context
import android.net.wifi.WifiManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import net.kuama.wifiMonitor.data.WifiStatus
import org.junit.Test

class WifiMonitorTest {

    @Test(expected = IllegalStateException::class)
    fun `it throws an IllegalStateException if WifiListenerBuilder is built without all parameters`() {
        // Arrange
        // Act
        WifiMonitor.WifiMonitorBuilder().build()
        // Assert
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `it sends a wifi status disconnected in the flow when wifi manager is receiving a disabled state`() =
        runBlockingTest {
            // Arrange
            val context = mockk<Context>(relaxed = true)
            val wifiManager = mockk<WifiManager>(relaxed = true)
            val permissionChecker = mockk<PermissionChecker>(relaxed = true)
            val wifiListener = mockk<WifiListener>(relaxed = true)

            every {
                wifiManager.wifiState
            } returns WifiManager.WIFI_STATE_DISABLED

            val wifiMonitor = WifiMonitor.WifiMonitorBuilder()
                .context(context)
                .listener(wifiListener)
                .wifiManager(wifiManager)
                .permissionChecker(permissionChecker)
                .build()

            val callbackSlot = slot<() -> Unit>()
            every {
                wifiListener.start(capture(callbackSlot))
            } answers {
                callbackSlot.captured.invoke()
            }

            // Act
            val callbackFlow = wifiMonitor.start()
            // Assert
            assert(callbackFlow.first() == WifiStatus(WifiStatus.State.DISCONNECTED)) {
                callbackFlow.first().state.name
            }
        }
}