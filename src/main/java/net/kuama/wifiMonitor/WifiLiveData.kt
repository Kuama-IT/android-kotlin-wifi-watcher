package net.kuama.wifiMonitor

import androidx.lifecycle.LiveData
import kotlinx.coroutines.*
import net.kuama.wifiMonitor.data.WifiStatus

class WifiLiveData constructor(private val monitor: WifiMonitor) :
    LiveData<WifiStatus>() {

    private var startJob: Job? = null

    override fun onActive() {
        super.onActive()
        startJob = CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            monitor.observe(::postValue)
        }
    }

    override fun onInactive() {
        super.onInactive()
        if (startJob?.isActive == true) {
            startJob?.cancel()
        }
        monitor.stop()
    }
}
