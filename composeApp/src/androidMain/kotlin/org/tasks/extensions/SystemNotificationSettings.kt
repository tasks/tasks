package org.tasks.extensions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import org.koin.mp.KoinPlatform

actual fun openSystemNotificationSettings() {
    val context = KoinPlatform.getKoin().get<Context>()
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
