package org.tasks.notifications

const val MAX_NOTIFICATIONS = 21

enum class CancelReason {
    COMPLETE,
    CLEANUP,
    DISMISS,
    DUE_DATE_CHANGE,
    EDIT,
    REMOTE_CLEAR,
    REMOTE_COMPLETION,
    REMOTE_DELETION,
    SNOOZE,
    STALE,
    TIMER,
    EVICTED,

    DISABLED,
}

interface Notifier {
    suspend fun cancel(id: Long, reason: CancelReason)

    suspend fun cancel(ids: List<Long>, reason: CancelReason)

    suspend fun cancelAll(reason: CancelReason)

    fun triggerNotifications()

    suspend fun updateTimerNotification()
}
