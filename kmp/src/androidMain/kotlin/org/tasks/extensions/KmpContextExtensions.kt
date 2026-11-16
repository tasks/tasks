package org.tasks.extensions

import android.content.Context
import android.content.Intent
import android.provider.Settings

fun Context.bringTasksToForeground() {
    packageManager.getLaunchIntentForPackage(packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ?.let { startActivity(it) }
}

val Context.isFirebaseTestLab: Boolean
    get() = Settings.System.getString(contentResolver, "firebase.test.lab") == "true"
