package org.tasks.scheduling

import com.todoroo.astrid.data.Task
import kotlinx.collections.immutable.toImmutableList
import org.tasks.data.TaskDao
import org.tasks.jobs.WorkManager
import org.tasks.time.DateTimeUtils
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshScheduler @Inject internal constructor(
        private val workManager: WorkManager,
        private val taskDao: TaskDao,
) {

    private val jobs: SortedSet<Long> = TreeSet()

    @Synchronized
    suspend fun scheduleAll() {
        for (task in taskDao.needsRefresh()) {
            scheduleRefresh(task)
        }
    }

    @Synchronized
    fun scheduleRefresh(task: Task) {
        if (task.hasDueDate()) {
            scheduleRefresh(task.dueDate)
        }
        if (task.hasStartDate()) {
            scheduleRefresh(task.hideUntil)
        }
    }

    @Synchronized
    fun scheduleNext() {
        val lapsed = jobs.headSet(DateTimeUtils.currentTimeMillis() + 1).toImmutableList()
        jobs.removeAll(lapsed)
        if (!jobs.isEmpty()) {
            workManager.scheduleRefresh(jobs.first())
        }
    }

    private fun scheduleRefresh(timestamp: Long) {
        val now = DateTimeUtils.currentTimeMillis()
        if (now < timestamp) {
            val upcoming = jobs.tailSet(now)
            val reschedule = upcoming.isEmpty() || timestamp < upcoming.first()
            jobs.add(timestamp)
            if (reschedule) {
                scheduleNext()
            }
        }
    }
}