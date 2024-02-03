package org.tasks.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.AndroidUtilities.preUpsideDownCake
import com.todoroo.astrid.core.BuiltInFilterExposer
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.Alarm
import org.tasks.data.LocationDao
import org.tasks.data.TaskDao
import org.tasks.intents.TaskIntents
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.receivers.CompleteTaskReceiver
import org.tasks.reminders.NotificationActivity
import org.tasks.reminders.SnoozeActivity
import org.tasks.reminders.SnoozeDialog
import org.tasks.themes.ColorProvider
import org.tasks.time.DateTime
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class NotificationManager @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val notificationDao: NotificationDao,
        private val taskDao: TaskDao,
        private val locationDao: LocationDao,
        private val localBroadcastManager: LocalBroadcastManager,
        private val notificationManager: ThrottledNotificationManager,
        private val markdownProvider: MarkdownProvider,
        private val permissionChecker: PermissionChecker,
) {
    private val colorProvider = ColorProvider(context, preferences)
    private val queue = NotificationLimiter(MAX_NOTIFICATIONS)

    @SuppressLint("CheckResult")
    suspend fun cancel(id: Long) {
        if (id == SUMMARY_NOTIFICATION_ID.toLong()) {
            cancel(notificationDao.getAll() + id)
        } else {
            cancel(listOf(id))
        }
    }

    @SuppressLint("CheckResult")
    suspend fun cancel(ids: Iterable<Long>) {
        for (id in ids) {
            notificationManager.cancel(id.toInt())
        }
        queue.remove(ids)
        notificationDao.deleteAll(ids.toList())
        notifyTasks(emptyList(), alert = false, nonstop = false, fiveTimes = false)
    }

    suspend fun restoreNotifications(cancelExisting: Boolean) {
        val notifications = notificationDao.getAllOrdered()
        if (cancelExisting) {
            for (notification in notifications) {
                notificationManager.cancel(notification.taskId.toInt())
            }
        }
        if (preferences.bundleNotifications() && notifications.size > 1) {
            updateSummary(
                    notify = false,
                    nonStop = false,
                    fiveTimes = false,
                    newNotifications = emptyList())
            createNotifications(
                    notifications,
                    alert = false,
                    nonstop = false,
                    fiveTimes = false,
                    useGroupKey = true)
        } else {
            createNotifications(
                    notifications,
                    alert = false,
                    nonstop = false,
                    fiveTimes = false,
                    useGroupKey = false)
            cancelSummaryNotification()
        }
    }

    suspend fun notifyTasks(
            newNotifications: List<Notification>,
            alert: Boolean,
            nonstop: Boolean,
            fiveTimes: Boolean) {
        val existingNotifications = notificationDao.getAllOrdered()
        notificationDao.insertAll(newNotifications)
        val totalCount = existingNotifications.size + newNotifications.size
        when {
            totalCount == 0 -> cancelSummaryNotification()
            totalCount == 1 -> {
                val notifications = existingNotifications + newNotifications
                createNotifications(notifications, alert, nonstop, fiveTimes, false)
                cancelSummaryNotification()
            }
            preferences.bundleNotifications() -> {
                updateSummary(
                        notify = false,
                        nonStop = false,
                        fiveTimes = false,
                        newNotifications = emptyList())
                if (existingNotifications.size == 1) {
                    createNotifications(
                            existingNotifications,
                            alert = false,
                            nonstop = false,
                            fiveTimes = false,
                            useGroupKey = true)
                }
                if (newNotifications.size == 1) {
                    createNotifications(newNotifications, alert, nonstop, fiveTimes, true)
                } else {
                    createNotifications(
                            newNotifications,
                            alert = false,
                            nonstop = false,
                            fiveTimes = false,
                            useGroupKey = true)
                    updateSummary(alert, nonstop, fiveTimes, newNotifications)
                }
            }
            else -> createNotifications(newNotifications, alert, nonstop, fiveTimes, false)
        }
        localBroadcastManager.broadcastRefresh()
    }

    private suspend fun createNotifications(
            notifications: List<Notification>,
            alert: Boolean,
            nonstop: Boolean,
            fiveTimes: Boolean,
            useGroupKey: Boolean
    ) {
        if (!permissionChecker.canNotify()) {
            Timber.w("Notifications disabled")
            return
        }
        var alert = alert
        for (notification in notifications) {
            val builder = getTaskNotification(notification)
            if (builder == null) {
                notificationManager.cancel(notification.taskId.toInt())
                notificationDao.delete(notification.taskId)
            } else {
                builder
                        .setGroup(if (useGroupKey) GROUP_KEY else notification.taskId.toString())
                        .setGroupAlertBehavior(
                                if (alert) NotificationCompat.GROUP_ALERT_CHILDREN else NotificationCompat.GROUP_ALERT_SUMMARY)
                notify(notification.taskId, builder, alert, nonstop, fiveTimes)
                val reminderTime = DateTime(notification.timestamp).endOfMinute().millis
                taskDao.setLastNotified(notification.taskId, reminderTime)
                alert = false
            }
        }
    }

    suspend fun notify(
            notificationId: Long,
            builder: NotificationCompat.Builder,
            alert: Boolean,
            nonstop: Boolean,
            fiveTimes: Boolean) {
        if (preUpsideDownCake()) {
            builder.setLocalOnly(!preferences.getBoolean(R.string.p_wearable_notifications, true))
        }
        if (AndroidUtilities.preOreo()) {
            if (alert) {
                builder
                        .setSound(preferences.ringtone)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(preferences.notificationDefaults)
            } else {
                builder.setDefaults(0).setTicker(null)
            }
        }
        val notification = builder.build()
        var ringTimes = if (fiveTimes) 5 else 1
        if (alert && nonstop) {
            notification.flags = notification.flags or NotificationCompat.FLAG_INSISTENT
            ringTimes = 1
        }
        if (preferences.usePersistentReminders()) {
            notification.flags = notification.flags or
                    NotificationCompat.FLAG_NO_CLEAR or
                    NotificationCompat.FLAG_ONGOING_EVENT
        }
        val deleteIntent = Intent(context, NotificationClearedReceiver::class.java)
        deleteIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        notification.deleteIntent = PendingIntent.getBroadcast(
            context,
            notificationId.toInt(),
            deleteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val evicted = queue.add(notificationId)
        if (evicted.size > 0) {
            cancel(evicted)
        }
        for (i in 0 until ringTimes) {
            if (i > 0) {
                notificationManager.pause(2000)
            }
            notificationManager.notify(notificationId.toInt(), notification)
        }
    }

    private suspend fun updateSummary(
            notify: Boolean, nonStop: Boolean, fiveTimes: Boolean, newNotifications: List<Notification>) {
        val tasks = taskDao.activeNotifications()
        val taskCount = tasks.size
        if (taskCount == 0) {
            cancelSummaryNotification()
            return
        }
        val taskIds = tasks.map { it.id }
        var maxPriority = 3
        val summaryTitle = context.resources.getQuantityString(R.plurals.task_count, taskCount, taskCount)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(summaryTitle)
        val titles: MutableList<String?> = ArrayList()
        val ticker: MutableList<String?> = ArrayList()
        for (task in tasks) {
            val title = task.title
            style.addLine(title)
            titles.add(title)
            maxPriority = min(maxPriority, task.priority)
        }
        for (notification in newNotifications) {
            tasks.find { it.id == notification.taskId }?.let { ticker.add(it.title) }
        }
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_DEFAULT)
                .setContentTitle(summaryTitle)
                .setContentText(
                        titles.joinToString(context.getString(R.string.list_separator_with_space)))
                .setShowWhen(true)
                .setSmallIcon(R.drawable.ic_done_all_white_24dp)
                .setStyle(style)
                .setColor(colorProvider.getPriorityColor(maxPriority, true))
                .setOnlyAlertOnce(false)
                .setContentIntent(
                        PendingIntent.getActivity(
                                context,
                                0,
                                TaskIntents.getTaskListIntent(context, BuiltInFilterExposer.getNotificationsFilter(context)),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                )
                .setGroupSummary(true)
                .setGroup(GROUP_KEY)
                .setTicker(
                        ticker.joinToString(context.getString(R.string.list_separator_with_space)))
                .setGroupAlertBehavior(
                        if (notify) NotificationCompat.GROUP_ALERT_SUMMARY else NotificationCompat.GROUP_ALERT_CHILDREN)
        notificationDao.latestTimestamp()?.let { builder.setWhen(it) }
        val snoozeIntent = SnoozeActivity.newIntent(context, taskIds)
        builder.addAction(
                R.drawable.ic_snooze_white_24dp,
                context.getString(R.string.snooze_all),
                PendingIntent.getActivity(
                    context,
                    0,
                    snoozeIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                )
        )
        notify(SUMMARY_NOTIFICATION_ID.toLong(), builder, notify, nonStop, fiveTimes)
    }

    suspend fun getTaskNotification(notification: Notification): NotificationCompat.Builder? {
        val id = notification.taskId
        val type = notification.type
        val `when` = notification.timestamp
        val task = taskDao.fetch(id)
        if (task == null) {
            Timber.e("Could not find %s", id)
            return null
        }

        // you're done, or not yours - don't sound, do delete
        if (task.isCompleted || task.isDeleted) {
            return null
        }

        // new task edit in progress
        if (isNullOrEmpty(task.title)) {
            return null
        }

        // it's hidden - don't sound, don't delete
        if (task.isHidden && type == Alarm.TYPE_RANDOM) {
            return null
        }

        // read properties
        val markdown = markdownProvider.markdown(force = true)
        val taskTitle = markdown.toMarkdown(task.title)
        val taskDescription = markdown.toMarkdown(task.notes)

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentTitle(taskTitle)
                .setColor(colorProvider.getPriorityColor(task.priority, true))
                .setSmallIcon(R.drawable.ic_check_white_24dp)
                .setWhen(`when`)
                .setOnlyAlertOnce(false)
                .setShowWhen(true)
                .setTicker(taskTitle)
        val intent = NotificationActivity.newIntent(context, taskTitle.toString(), id, task.readOnly)
        builder.setContentIntent(
                PendingIntent.getActivity(
                    context,
                    id.toInt(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
        )
        if (type == Alarm.TYPE_GEO_ENTER || type == Alarm.TYPE_GEO_EXIT) {
            val place = locationDao.getPlace(notification.location!!)
            if (place != null) {
                builder.setContentText(
                        context.getString(
                                if (type == Alarm.TYPE_GEO_ENTER) R.string.location_arrived else R.string.location_departed,
                                place.displayName))
            }
        } else if (taskDescription?.isNotBlank() == true) {
            builder
                    .setContentText(taskDescription)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(taskDescription))
        }
        val completeIntent = Intent(context, CompleteTaskReceiver::class.java)
        completeIntent.putExtra(CompleteTaskReceiver.TASK_ID, id)
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            completeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val completeAction = NotificationCompat.Action.Builder(
                R.drawable.ic_check_white_24dp,
                context.getString(R.string.rmd_NoA_done),
                completePendingIntent)
                .build()
        val snoozeIntent = SnoozeActivity.newIntent(context, id)
        val snoozePendingIntent = PendingIntent.getActivity(
            context,
            id.toInt(),
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val wearableExtender = NotificationCompat.WearableExtender()
        if (!task.readOnly) {
            wearableExtender.addAction(completeAction)
        }
        for (snoozeOption in SnoozeDialog.getSnoozeOptions(preferences)) {
            val timestamp = snoozeOption.dateTime.millis
            val wearableIntent = SnoozeActivity.newIntent(context, id)
            wearableIntent.action = String.format("snooze-%s-%s", id, timestamp)
            wearableIntent.putExtra(SnoozeActivity.EXTRA_SNOOZE_TIME, timestamp)
            val wearablePendingIntent = PendingIntent.getActivity(
                context,
                id.toInt(),
                wearableIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            wearableExtender.addAction(
                    NotificationCompat.Action.Builder(
                            R.drawable.ic_snooze_white_24dp,
                            context.getString(snoozeOption.resId),
                            wearablePendingIntent)
                            .build())
        }
        if (!task.readOnly) {
            builder.addAction(completeAction)
        }
        return builder
                .addAction(
                        R.drawable.ic_snooze_white_24dp,
                        context.getString(R.string.rmd_NoA_snooze),
                        snoozePendingIntent)
                .extend(wearableExtender)
    }

    private fun cancelSummaryNotification() {
        notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_DEFAULT = "notifications"
        const val NOTIFICATION_CHANNEL_TASKER = "notifications_tasker"
        const val NOTIFICATION_CHANNEL_TIMERS = "notifications_timers"
        const val NOTIFICATION_CHANNEL_MISCELLANEOUS = "notifications_miscellaneous"
        const val MAX_NOTIFICATIONS = 21
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val SUMMARY_NOTIFICATION_ID = 0
        private const val GROUP_KEY = "tasks"
    }
}