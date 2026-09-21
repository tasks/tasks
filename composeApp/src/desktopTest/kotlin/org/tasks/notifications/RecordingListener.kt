package org.tasks.notifications

internal class RecordingListener : NotificationActionListener {
    val actions = mutableListOf<Pair<Long, NotificationAction>>()
    val dismissals = mutableListOf<Long>()
    val evictions = mutableListOf<Long>()

    override fun onAction(taskId: Long, action: NotificationAction) {
        actions.add(taskId to action)
    }

    override fun onDismissed(taskId: Long) {
        dismissals.add(taskId)
    }

    override fun onEvicted(taskId: Long) {
        evictions.add(taskId)
    }
}
