package org.tasks.service

import org.tasks.notifications.DesktopNotifier

internal class DesktopCleanup(
    private val notifier: DesktopNotifier,
) : TaskCleanup {
    override suspend fun cleanup(tasks: List<Long>) {
        if (tasks.isEmpty()) {
            return
        }
        notifier.cancelDeleted(tasks)
    }
}
