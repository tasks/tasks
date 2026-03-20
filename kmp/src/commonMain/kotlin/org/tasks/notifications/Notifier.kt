package org.tasks.notifications

interface Notifier {
    suspend fun cancel(id: Long)

    suspend fun cancel(ids: Iterable<Long>)

    fun triggerNotifications()

    suspend fun updateTimerNotification()
}
