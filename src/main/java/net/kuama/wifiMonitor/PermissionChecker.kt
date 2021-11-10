package net.kuama.wifiMonitor

import android.Manifest
import android.content.Context
import androidx.core.content.ContextCompat

/**
 * This class allows to check if any permission from the manifest has been granted.
 */
class PermissionChecker(private val context: Context) {
    class Builder {
        private var context: Context? = null
        fun context(context: Context) = apply { this.context = context }

        fun build() = PermissionChecker(checkNotNull(context) { "Please provide a valid context" })
    }

    /**
     * Check whether a permission has been granted to the application.
     *
     * @param [permission] Permission identifier from [Manifest.permission].
     */
    fun check(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == androidx.core.content.PermissionChecker.PERMISSION_GRANTED
}
