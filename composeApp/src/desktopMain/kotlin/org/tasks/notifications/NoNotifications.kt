package org.tasks.notifications

import co.touchlab.kermit.Logger

private const val TAG = "NoNotifications"

object NoNotifications : Notifier {
    override suspend fun cancel(id: Long, reason: CancelReason) = logIgnored("cancel $id ($reason)")

    override suspend fun cancel(ids: List<Long>, reason: CancelReason) =
        logIgnored("cancel $ids ($reason)")

    override suspend fun cancelAll(reason: CancelReason) = logIgnored("cancel everything ($reason)")

    override fun triggerNotifications() = logIgnored("trigger")

    override suspend fun updateTimerNotification() = logIgnored("update the timer notification")

    private fun logIgnored(what: String) =
        Logger.d(tag = TAG) { "No notifier on desktop yet, ignoring request to $what" }
}
