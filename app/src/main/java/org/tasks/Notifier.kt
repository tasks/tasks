package org.tasks

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.voice.VoiceOutputAssistant
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import org.tasks.data.Alarm
import org.tasks.data.Alarm.Companion.TYPE_GEO_ENTER
import org.tasks.data.Alarm.Companion.TYPE_GEO_EXIT
import org.tasks.data.Geofence
import org.tasks.data.TaskDao
import org.tasks.intents.TaskIntents
import org.tasks.notifications.AudioManager
import org.tasks.notifications.Notification
import org.tasks.notifications.NotificationManager
import org.tasks.notifications.TelephonyManager
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.time.DateTimeUtils
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.min

class Notifier @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val taskDao: TaskDao,
        private val notificationManager: NotificationManager,
        private val telephonyManager: TelephonyManager,
        private val audioManager: AudioManager,
        private val voiceOutputAssistant: VoiceOutputAssistant,
        private val preferences: Preferences) {

    private val colorProvider: ColorProvider = ColorProvider(context, preferences)

    suspend fun triggerFilterNotification(filter: Filter) {
        val tasks = taskDao.fetchFiltered(filter)
        val count = tasks.size
        if (count == 0) {
            return
        }
        val intent = TaskIntents.getTaskListIntent(context, filter)
        val pendingIntent = PendingIntent.getActivity(
            context,
            filter.title.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val summaryTitle = context.resources.getQuantityString(R.plurals.task_count, count, count)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(summaryTitle)
        var maxPriority = 3
        for (task in tasks) {
            style.addLine(task.title)
            maxPriority = min(maxPriority, task.priority)
        }
        val builder = NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_TASKER)
                .setSmallIcon(R.drawable.ic_done_all_white_24dp)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setTicker(summaryTitle)
                .setContentTitle(summaryTitle)
                .setContentText(filter.title)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setWhen(DateTimeUtils.currentTimeMillis())
                .setShowWhen(true)
                .setColor(colorProvider.getPriorityColor(maxPriority, true))
                .setGroupSummary(true)
                .setGroup(filter.title)
                .setStyle(style)
        notificationManager.notify(filter.title.hashCode().toLong(), builder, alert = true, nonstop = false, fiveTimes = false)
    }

    suspend fun triggerNotifications(place: Long, geofences: List<Geofence>, arrival: Boolean) =
            geofences
                    .filter { if (arrival) it.isArrival else it.isDeparture }
                    .map {
                        Notification().apply {
                            taskId = it.task
                            type = if (arrival) TYPE_GEO_ENTER else TYPE_GEO_EXIT
                            timestamp = DateTimeUtils.currentTimeMillis()
                            location = place
                        }
                    }
                    .let { triggerNotifications(it) }

    suspend fun triggerNotifications(entries: List<Notification>) {
        var ringFiveTimes = false
        var ringNonstop = false
        val notifications = entries
                .filter {
                    taskDao.fetch(it.taskId)
                            ?.let { task ->
                                if (it.type != Alarm.TYPE_RANDOM) {
                                    ringFiveTimes = ringFiveTimes or task.isNotifyModeFive
                                    ringNonstop = ringNonstop or task.isNotifyModeNonstop
                                }
                                notificationManager.getTaskNotification(it) != null
                            }
                            ?: false
                }
                .takeLast(NotificationManager.MAX_NOTIFICATIONS)

        if (notifications.isEmpty()) {
            return
        }

        Timber.d("Triggering $notifications")

        notificationManager.notifyTasks(notifications, true, ringNonstop, ringFiveTimes)

        if (preferences.getBoolean(R.string.p_voiceRemindersEnabled, false)
                && !ringNonstop
                && !audioManager.notificationsMuted()
                && telephonyManager.callStateIdle()) {
            notifications
                    .mapNotNull {
                        notificationManager.getTaskNotification(it)?.build()?.tickerText?.toString()
                    }
                    .forEach {
                        delay(2000)
                        voiceOutputAssistant.speak(it)
                    }
        }
    }
}