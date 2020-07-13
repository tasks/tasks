package org.tasks.data

import javax.inject.Inject

@Deprecated("use coroutines")
class AlarmDaoBlocking @Inject constructor(private val dao: AlarmDao) {
    fun getAlarms(taskId: Long): List<Alarm> = runBlocking {
        dao.getAlarms(taskId)
    }

    fun insert(alarms: Iterable<Alarm>) = runBlocking {
        dao.insert(alarms)
    }
}