package org.tasks.jobs

import com.google.common.collect.Ordering
import com.google.common.collect.TreeMultimap
import com.google.common.primitives.Ints
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationQueue @Inject constructor(
    private val preferences: Preferences,
    private val workManager: WorkManager
) {
    private val jobs =
        TreeMultimap.create<Long, AlarmEntry>(Ordering.natural()) { l, r ->
            Ints.compare(l.hashCode(), r.hashCode())
        }

    @Synchronized
    fun add(entry: AlarmEntry) = add(listOf(entry))

    @Synchronized
    fun add(entries: Iterable<AlarmEntry>) {
        val originalFirstTime = firstTime()
        entries.forEach { jobs.put(it.time, it) }
        if (originalFirstTime != firstTime()) {
            scheduleNext(true)
        }
    }

    @Synchronized
    fun clear() {
        jobs.clear()
        workManager.cancelNotifications()
    }

    fun cancelForTask(taskId: Long) {
        val firstTime = firstTime()
        jobs.values().filter { it.taskId == taskId }.forEach { remove(listOf(it)) }
        if (firstTime != firstTime()) {
            scheduleNext(true)
        }
    }

    @get:Synchronized
    val overdueJobs: List<AlarmEntry>
        get() = jobs.keySet()
            .headSet(DateTime().startOfMinute().plusMinutes(1).millis)
            .flatMap { jobs[it] }

    @Synchronized
    fun scheduleNext() = scheduleNext(false)

    private fun scheduleNext(cancelCurrent: Boolean) {
        if (jobs.isEmpty) {
            if (cancelCurrent) {
                workManager.cancelNotifications()
            }
        } else {
            workManager.scheduleNotification(nextScheduledTime())
        }
    }

    private fun firstTime() = if (jobs.isEmpty) 0L else jobs.asMap().firstKey()

    fun nextScheduledTime(): Long {
        val next = firstTime()
        return if (next > 0) preferences.adjustForQuietHours(next) else 0
    }

    fun size() = jobs.size()

    fun getJobs() = jobs.values().toList()

    fun isEmpty() = jobs.isEmpty

    @Synchronized
    fun remove(entries: List<AlarmEntry>): Boolean {
        var success = true
        for (entry in entries) {
            success = success and (!jobs.containsEntry(entry.time, entry) || jobs.remove(
                entry.time,
                entry
            ))
        }
        return success
    }
}