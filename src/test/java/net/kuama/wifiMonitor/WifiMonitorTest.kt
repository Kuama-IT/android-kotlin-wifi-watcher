package net.kuama.wifiMonitor

import android.content.Context
import android.net.wifi.WifiManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import net.kuama.wifiMonitor.data.WifiStatus
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WifiMonitorTest {
    @Test
    fun `it sends a wifi status disconnected in the flow when wifi manager is receiving a disabled state`() =
        runBlockingTest {
            // Arrange
            val context = mockk<Context>(relaxed = true)
            val wifiManager = mockk<WifiManager>(relaxed = true)
            val permissionChecker = mockk<PermissionChecker>(relaxed = true)
            val wifiListener = mockk<WifiListener>(relaxed = true)

            every { wifiManager.wifiState } returns WifiManager.WIFI_STATE_DISABLED

            val wifiMonitor = WifiMonitor.Builder()
                .listener(wifiListener)
                .wifiManager(wifiManager)
                .permissionChecker(permissionChecker)
                .build(context)

            every {
                wifiListener.listen(context)
            } answers {
                flowOf(Unit)
            }

            // Act
            val callbackFlow = wifiMonitor.monitor()
            // Assert
            assert(callbackFlow.first() == WifiStatus(WifiStatus.State.DISCONNECTED)) {
                callbackFlow.first().state.name
            }
        }
}
