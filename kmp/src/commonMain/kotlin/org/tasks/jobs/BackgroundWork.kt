package org.tasks.jobs

import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Task
import org.tasks.sync.SyncSource
import org.tasks.time.DateTimeUtils2

interface BackgroundWork {
    fun updateCalendar(task: Task)

    suspend fun scheduleRefresh(timestamp: Long = DateTimeUtils2.currentTimeMillis() + 5_000)

    suspend fun sync(source: SyncSource)

    suspend fun scheduleBlogFeedCheck()

    fun updateBackgroundSync() {}

    fun migrateLocalTasks(account: CaldavAccount) {}
}
