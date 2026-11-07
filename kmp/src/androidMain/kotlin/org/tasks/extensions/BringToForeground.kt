package org.tasks.extensions

import android.content.Context
import android.content.Intent

fun Context.bringTasksToForeground() {
    packageManager.getLaunchIntentForPackage(packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ?.let { startActivity(it) }
}
