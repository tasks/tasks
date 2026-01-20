package org.tasks.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class Device @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
    fun hasCamera() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)

    fun hasMicrophone() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

    fun voiceInputAvailable(): Boolean {
        val pm = context.packageManager
        val activities =
            pm.queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0)
        return activities.isNotEmpty()
    }
}
