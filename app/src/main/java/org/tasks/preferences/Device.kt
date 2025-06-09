package org.tasks.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.BuildConfig
import java.util.Locale
import javax.inject.Inject

class Device @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionChecker: PermissionChecker,
) {
    @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
    fun hasCamera() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)

    fun hasMicrophone() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

    fun voiceInputAvailable(): Boolean {
        val pm = context.packageManager
        val activities =
            pm.queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0)
        return (activities.size != 0)
    }

    val debugInfo: String
        get() = """
            ----------
            Tasks: ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR} build ${BuildConfig.VERSION_CODE})
            Android: ${Build.VERSION.RELEASE} (${Build.DISPLAY})
            Locale: ${Locale.getDefault()}
            Model: ${Build.MANUFACTURER} ${Build.MODEL}
            Product: ${Build.PRODUCT} (${Build.DEVICE})
            Kernel: ${System.getProperty("os.version")} (${Build.VERSION.INCREMENTAL})
            ----------
            notifications: ${permissionChecker.hasNotificationPermission()}
            reminders: ${permissionChecker.hasAlarmsAndRemindersPermission()}
            background location: ${permissionChecker.canAccessBackgroundLocation()}
            foreground location: ${permissionChecker.canAccessForegroundLocation()}
            calendar: ${permissionChecker.canAccessCalendars()}
            ----------
        """.trimIndent()
}
