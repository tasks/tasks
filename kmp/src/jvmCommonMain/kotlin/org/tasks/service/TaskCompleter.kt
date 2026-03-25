package org.tasks.service

import co.touchlab.kermit.Logger
import com.todoroo.astrid.repeats.RepeatTaskHelper
import com.todoroo.astrid.repeats.RepeatTaskHelper.Companion.computePreviousDueDate
import org.tasks.audio.SoundPlayer
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.calendars.CalendarHelper
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.CompletionDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.notifications.Notifier
import org.tasks.time.DateTimeUtils2.currentTimeMillis

private const val TAG = "TaskCompleter"

class TaskCompleter(
    private val taskDao: TaskDao,
    private val taskSaver: TaskSaver,
    private val notifier: Notifier,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val repeatTaskHelper: RepeatTaskHelper,
    private val caldavDao: CaldavDao,
    private val calendarHelper: CalendarHelper,
    private val completionDao: CompletionDao,
    private val soundPlayer: SoundPlayer,
) {
    suspend fun setComplete(taskId: Long, completed: Boolean = true) =
        taskDao
            .fetch(taskId)
            ?.let { setComplete(it, completed) }
            ?: Logger.e(tag = TAG) { "Could not find task $taskId" }

    suspend fun setComplete(item: Task, completed: Boolean, includeChildren: Boolean = true) {
        val completionDate = if (completed) currentTimeMillis() else 0L
        ArrayList<Task?>()
            .apply {
                if (includeChildren) {
                    addAll(taskDao.getChildren(item.id).let { taskDao.fetch(it) })
                }
                if (!completed) {
                    addAll(taskDao.getParents(item.id).let { taskDao.fetch(it) })
                }
                add(item)
            }
            .filterNotNull()
            .filter { it.isCompleted != completionDate > 0 }
            .filterNot { it.readOnly }
            .let { tasks ->
                setComplete(tasks, completionDate)
            }
    }

    suspend fun setComplete(tasks: List<Task>, completionDate: Long) {
        if (tasks.isEmpty()) {
            return
        }
        tasks.forEach { notifier.cancel(it.id) }
        val completed = completionDate > 0
        Logger.d(TAG) { "Completing $tasks" }
        completionDao.complete(
            tasks = tasks,
            completionDate = completionDate,
            afterSave = { updated ->
                updated.forEach { saved ->
                    val original = tasks.find { it.id == saved.id }
                    taskSaver.afterSave(saved, original)
                }
                updated.forEach { task ->
                    if (completed && task.isRecurring) {
                        calendarHelper.updateEvent(task)

                        if (caldavDao.getAccountForTask(task.id)?.isSuppressRepeatingTasks != true) {
                            repeatTaskHelper.handleRepeat(task)
                            if (task.completionDate == 0L) {
                                setComplete(task, false)
                            }
                        }
                    }
                }
            }
        )
        if (completed) {
            soundPlayer.playCompletionSound()
        }
        refreshBroadcaster.broadcastRefresh()
    }
}
