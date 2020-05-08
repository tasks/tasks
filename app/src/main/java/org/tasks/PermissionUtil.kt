package org.tasks

import android.content.pm.PackageManager.PERMISSION_GRANTED

object PermissionUtil {
    @JvmStatic
    fun verifyPermissions(grantResults: IntArray) =
            grantResults.isNotEmpty() && grantResults.all { it == PERMISSION_GRANTED }
}