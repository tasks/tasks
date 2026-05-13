package org.tasks.audio

import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT
import android.media.RingtoneManager
import org.tasks.notifications.NotificationManager
import org.tasks.preferences.Preferences

class AndroidSoundPlayer(
    private val context: Context,
    private val preferences: Preferences,
    private val notificationManager: NotificationManager,
) : SoundPlayer {
    override suspend fun playCompletionSound() {
        if (notificationManager.currentInterruptionFilter != INTERRUPTION_FILTER_ALL) {
            return
        }
        if (preferences.isCurrentlyQuietHours()) {
            return
        }
        preferences.completionSound?.let { uri ->
            RingtoneManager.getRingtone(context, uri)
                ?.apply {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(USAGE_NOTIFICATION_EVENT)
                        .build()
                }
                ?.play()
        }
    }
}
