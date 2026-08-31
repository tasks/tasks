package org.tasks.notifications

import org.jetbrains.compose.resources.getString
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.di.appName
import org.tasks.extensions.guarded
import org.tasks.extensions.truncate
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.snoozed_reminder

private const val TAG = "NotificationContent"

internal object NotificationContent {
    fun title(task: Task): String =
    truncate(task.title?.takeIf { it.isNotBlank() } ?: appName, MAX_TITLE_LENGTH)

    suspend fun body(task: Task, notification: Notification): String? {
    val notes = task.notes?.takeIf { it.isNotBlank() }?.let { truncate(it, MAX_BODY_LENGTH) }
    if (notification.type != Alarm.TYPE_SNOOZE) {
        return notes
    }

    return guarded(TAG, "Failed to resolve notification body", notes, warnOnly = true) {
        getString(Res.string.snoozed_reminder)
    }
    }

    fun actionsFor(task: Task, backend: PlatformNotifications): List<NotificationAction> =
        buildList {
            if (backend.supportsOpen) {
                add(NotificationAction.OPEN)
            }
            if (backend.supportsActions) {
                if (!task.readOnly) {
                    add(NotificationAction.COMPLETE)
                }
                add(NotificationAction.SNOOZE)
            }
        }

    const val MAX_TITLE_LENGTH = 200
    const val MAX_BODY_LENGTH = 1_000

    internal fun truncate(value: String, max: Int): String = value.truncate(max, "…")
}
