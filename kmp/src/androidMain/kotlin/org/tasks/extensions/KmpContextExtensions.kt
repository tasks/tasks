package org.tasks.extensions

import android.content.Context
import android.os.Build
import android.provider.Settings

val Context.isFirebaseTestLab: Boolean
    get() = Settings.System.getString(contentResolver, "firebase.test.lab") == "true"

val Context.installerPackageName: String?
    get() = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
    } catch (e: Exception) {
        null
    }
