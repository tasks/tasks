package org.tasks.data

import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Deprecated("use coroutines")
class AlarmDaoBlocking @Inject constructor(private val dao: AlarmDao) {
    fun getActiveAlarms(): List<Alarm> = runBlocking {
        dao.getActiveAlarms()
    }

    fun getActiveAlarms(taskId: Long): List<Alarm> = runBlocking {
        dao.getActiveAlarms(taskId)
    }

    fun getAlarms(taskId: Long): List<Alarm> = runBlocking {
        dao.getAlarms(taskId)
    }

    fun delete(alarm: Alarm) = runBlocking {
        dao.delete(alarm)
    }

    fun insert(alarm: Alarm): Long = runBlocking {
        dao.insert(alarm)
    }

    fun insert(alarms: Iterable<Alarm>) = runBlocking {
        dao.insert(alarms)
    }
}