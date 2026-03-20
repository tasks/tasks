package org.tasks.jobs

import org.tasks.data.entity.Task
import org.tasks.sync.SyncSource

interface BackgroundWork {
    fun updateCalendar(task: Task)

    suspend fun scheduleRefresh(timestamp: Long = org.tasks.time.DateTimeUtils2.currentTimeMillis() + 5_000)

    suspend fun sync(source: SyncSource)
}
