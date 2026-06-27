/**
 * WearNotificationManager.kt — Local reminder notifications on Wear OS.
 *
 * ## How it works
 * 1. When a task with `reminder = true` is saved, [scheduleReminder] sets
 *    an exact [AlarmManager] alarm at the task's `reminderTime` (or `dueDate`).
 * 2. When the alarm fires, [ReminderReceiver] (inner broadcast receiver)
 *    creates and posts a [NotificationCompat] notification with:
 *    - A "Complete" action that marks the task done.
 *    - A content tap that opens [MainActivity].
 * 3. [cancelReminder] removes both the alarm and any existing notification.
 *
 * ## Boot persistence
 * Alarms are lost on reboot.  [BootReceiver] calls [rescheduleAll] after
 * `ACTION_BOOT_COMPLETED` to re-register every pending reminder.
 *
 * ## Singleton
 * Accessed via [WearNotificationManager.getInstance].
 */
package org.tasks.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.data.local.TaskEntity
import org.tasks.data.local.WearDatabase
import org.tasks.presentation.MainActivity
import timber.log.Timber

/**
 * Manages local notifications on the Wear OS device.
 * Handles scheduling, displaying and cancelling task reminder notifications.
 */
class WearNotificationManager private constructor(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "wear_task_reminders"
        const val CHANNEL_NAME = "Task Reminders"
        private const val ACTION_REMINDER = "org.tasks.wear.REMINDER"
        private const val ACTION_COMPLETE = "org.tasks.wear.COMPLETE_TASK"
        private const val EXTRA_TASK_ID = "extra_task_id"
        private const val EXTRA_TASK_TITLE = "extra_task_title"
        private const val NOTIFICATION_ID_OFFSET = 10000

        @Volatile
        private var instance: WearNotificationManager? = null

        fun getInstance(context: Context): WearNotificationManager {
            return instance ?: synchronized(this) {
                instance ?: WearNotificationManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Create notification channel. Must be called at app startup.
     */
    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders for tasks with due dates"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
        }
        val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        systemManager.createNotificationChannel(channel)
        Timber.d("Wear notification channel created")
    }

    /**
     * Check if we have permission to post notifications.
     */
    fun canNotify(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Schedule a reminder for a task.
     * @param task The task to remind about
     */
    fun scheduleReminder(task: TaskEntity) {
        if (!task.reminder) {
            cancelReminder(task.id)
            return
        }

        // Determine when to fire the reminder
        val triggerTime = task.reminderTime
            ?: task.dueTime
            ?: task.dueDate
            ?: return // No time to schedule

        if (triggerTime <= System.currentTimeMillis()) {
            // Already past due - fire immediately
            showNotification(task.id, task.title)
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent,
            )
            Timber.d("Scheduled reminder for task '${task.title}' at $triggerTime")
        } catch (e: SecurityException) {
            // Fallback: try inexact alarm
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent,
                )
                Timber.w("Used inexact alarm for task '${task.title}' (exact alarm permission denied)")
            } catch (e2: Exception) {
                Timber.e(e2, "Failed to schedule any alarm for task '${task.title}'")
            }
        }
    }

    /**
     * Cancel a scheduled reminder for a task.
     */
    fun cancelReminder(taskId: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Timber.d("Cancelled reminder for task $taskId")
        }
        // Also cancel any shown notification
        notificationManager.cancel(taskId.hashCode() + NOTIFICATION_ID_OFFSET)
    }

    /**
     * Show a notification for a task.
     */
    fun showNotification(taskId: String, title: String) {
        if (!canNotify()) {
            Timber.w("Cannot show notification - permission denied")
            return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // Complete action
        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 1000,
            completeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Task reminder")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                "Done",
                completePendingIntent,
            )
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .build()

        try {
            notificationManager.notify(
                taskId.hashCode() + NOTIFICATION_ID_OFFSET,
                notification,
            )
            Timber.d("Showed notification for task '$title' (id: $taskId)")
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException posting notification")
        }
    }

    /**
     * Reschedule all reminders (e.g., after boot or app update).
     */
    fun rescheduleAll() {
        scope.launch {
            try {
                val db = WearDatabase.getInstance(context)
                val tasks = db.taskDao().getTasksWithReminders()
                var scheduled = 0
                for (task in tasks) {
                    val triggerTime = task.reminderTime ?: task.dueTime ?: task.dueDate
                    if (triggerTime != null && triggerTime > System.currentTimeMillis()) {
                        scheduleReminder(task)
                        scheduled++
                    }
                }
                Timber.d("Rescheduled $scheduled reminders out of ${tasks.size} tasks with reminders")
            } catch (e: Exception) {
                Timber.e(e, "Failed to reschedule reminders")
            }
        }
    }

    /**
     * BroadcastReceiver that fires when a reminder alarm triggers or when a user acts on a notification.
     */
    class ReminderReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return

            when (intent.action) {
                ACTION_REMINDER -> {
                    val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task reminder"
                    Timber.d("Reminder triggered for task: $taskId ($title)")
                    getInstance(context).showNotification(taskId, title)
                }
                ACTION_COMPLETE -> {
                    Timber.d("Complete action for task: $taskId")
                    // Cancel the notification
                    NotificationManagerCompat.from(context)
                        .cancel(taskId.hashCode() + NOTIFICATION_ID_OFFSET)

                    // Mark task as completed in database
                    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                    scope.launch {
                        try {
                            val db = WearDatabase.getInstance(context)
                            db.taskDao().setCompleted(taskId, true, System.currentTimeMillis())
                            Timber.d("Task $taskId marked complete from notification")
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to complete task $taskId from notification")
                        }
                    }
                }
                Intent.ACTION_BOOT_COMPLETED -> {
                    Timber.d("Boot completed - rescheduling all reminders")
                    getInstance(context).rescheduleAll()
                }
            }
        }
    }
}


