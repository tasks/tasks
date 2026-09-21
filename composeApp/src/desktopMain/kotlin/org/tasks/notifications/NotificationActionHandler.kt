package org.tasks.notifications

import co.touchlab.kermit.Logger
import com.todoroo.astrid.alarms.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tasks.TaskEditDestination
import org.tasks.TaskRequests
import org.tasks.data.dao.TaskDao
import org.tasks.extensions.guarded
import org.tasks.requestForeground
import org.tasks.service.TaskCompleter

private const val TAG = "NotificationActions"

class NotificationActionHandler(
    private val scope: CoroutineScope,
    private val taskDao: TaskDao,
    private val alarmService: () -> AlarmService,
    private val taskCompleter: TaskCompleter,
    private val notifier: () -> Notifier,
    private val taskRequests: TaskRequests,

    private val requestForeground: () -> Unit = ::requestForeground,
) : NotificationActionListener {
    override fun onAction(taskId: Long, action: NotificationAction) {
        Logger.d(tag = TAG) { "$action on $taskId" }
        scope.launch {
            guarded(TAG, "Failed to handle $action on $taskId", Unit) {
                when (action) {
                    NotificationAction.OPEN -> open(taskId)

                    NotificationAction.COMPLETE -> taskCompleter.setComplete(taskId)
                    NotificationAction.SNOOZE -> snooze(taskId)
                }
            }
        }
    }

    override fun onDismissed(taskId: Long) {
        Logger.d(tag = TAG) { "Dismissed $taskId" }
        scope.launch {
            guarded(TAG, "Failed to clear $taskId", Unit) {
                notifier().cancel(taskId, CancelReason.DISMISS)
                alarmService().markDismissed(listOf(taskId))
            }
        }
    }

    override fun onEvicted(taskId: Long) {
        Logger.d(tag = TAG) { "Evicted $taskId" }
        scope.launch {
            guarded(TAG, "Failed to clear $taskId", Unit) {
                notifier().cancel(taskId, CancelReason.EVICTED)
            }
        }
    }

    private suspend fun open(taskId: Long) {
        val task = taskDao.fetch(taskId) ?: return logMissing(taskId)
        requestForeground.invoke()

        if (taskRequests.open(TaskEditDestination(taskId = task.id, remoteId = task.uuid))) {
            notifier().cancel(taskId, CancelReason.DISMISS)
        } else {
            Logger.w(tag = TAG) { "Nothing opened for $taskId, leaving the notification up" }
        }
    }

    private suspend fun snooze(taskId: Long) {
        taskDao.fetch(taskId) ?: return logMissing(taskId)
        requestForeground.invoke()
        taskRequests.snooze(taskId)
    }

    private fun logMissing(taskId: Long) = Logger.w(tag = TAG) { "No task $taskId" }
}
