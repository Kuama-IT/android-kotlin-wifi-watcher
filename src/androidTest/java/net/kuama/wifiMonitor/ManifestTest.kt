package net.kuama.wifiSpy

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test

class ManifestTest {

    @Test
    fun manifest_contains_needed_permissions() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = appContext.packageManager.getPackageInfo(
            appContext.packageName,
            PackageManager.GET_PERMISSIONS
        )


        Assert.assertEquals(
            packageInfo.requestedPermissions.contains(Manifest.permission.ACCESS_NETWORK_STATE),
            true
        )
    }
}