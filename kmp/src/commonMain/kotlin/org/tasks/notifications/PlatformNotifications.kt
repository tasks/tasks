package org.tasks.notifications

import org.jetbrains.compose.resources.getString
import org.tasks.extensions.guarded
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.action_open
import tasks.kmp.generated.resources.rmd_NoA_done
import tasks.kmp.generated.resources.rmd_NoA_snooze

enum class NotificationAction {
    OPEN,
    COMPLETE,
    SNOOZE;

    val key: String
        get() = when (this) {
            OPEN -> "open"
            COMPLETE -> "complete"
            SNOOZE -> "snooze"
        }

    val fallbackLabel: String
        get() = when (this) {
            OPEN -> "Open"
            COMPLETE -> "Complete"
            SNOOZE -> "Snooze"
        }

    companion object {
        fun fromKey(key: String): NotificationAction? = entries.firstOrNull { it.key == key }

        suspend fun labelOrNull(action: NotificationAction): String? = guarded(
            tag = "NotificationAction",
            what = "Failed to resolve the $action label",
            fallback = null,
            warnOnly = true,
        ) {
            when (action) {
                OPEN -> getString(Res.string.action_open)
                COMPLETE -> getString(Res.string.rmd_NoA_done)
                SNOOZE -> getString(Res.string.rmd_NoA_snooze)
            }
        }

        suspend fun label(action: NotificationAction): String =
            labelOrNull(action) ?: action.fallbackLabel
    }
}

enum class Alert {
    DEFAULT,

    QUIET,

    SUPPRESSED;

    val sound: Boolean get() = this == DEFAULT
}

sealed interface DeliveredQuery {
    data class Known(val taskIds: Set<Long>) : DeliveredQuery

    data object Unknown : DeliveredQuery
}

enum class NotificationPermission {
    GRANTED,

    NOT_DETERMINED,

    DENIED,
}

interface PlatformNotifications {
    val supportsActions: Boolean

    val supportsOpen: Boolean get() = true

    suspend fun show(
        taskId: Long,
        title: String,
        body: String?,
        actions: List<NotificationAction>,
        alert: Alert = Alert.DEFAULT,
    ): Boolean

    val actionsSurviveRestart: Boolean get() = true

    suspend fun delivered(): DeliveredQuery = DeliveredQuery.Unknown

    suspend fun platformId(taskId: Long): Long? = null

    suspend fun adopt(platformIds: Map<Long, Long>) = Unit

    suspend fun dismiss(taskIds: List<Long>): Set<Long>

    fun close(): Boolean

    suspend fun permission(): NotificationPermission = NotificationPermission.GRANTED

    suspend fun requestPermission(): Boolean = true
}

interface NotificationActionListener {
    fun onAction(taskId: Long, action: NotificationAction)

    fun onDismissed(taskId: Long)

    fun onEvicted(taskId: Long)
}
