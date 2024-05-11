package org.tasks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.todoroo.astrid.data.Task
import org.tasks.data.Alarm.Companion.TYPE_SNOOZE

@Dao
interface AlarmDao {
    @Query("""
SELECT alarms.*
FROM alarms
         INNER JOIN tasks ON tasks._id = alarms.task
WHERE tasks.completed = 0
  AND tasks.deleted = 0
""")
    suspend fun getActiveAlarms(): List<Alarm>

    @Query("""
SELECT alarms.*
FROM alarms
         INNER JOIN tasks ON tasks._id = alarms.task
WHERE tasks._id = :taskId
  AND tasks.completed = 0
  AND tasks.deleted = 0
""")
    suspend fun getActiveAlarms(taskId: Long): List<Alarm>

    @Query("SELECT * FROM alarms WHERE type = $TYPE_SNOOZE AND task IN (:taskIds)")
    suspend fun getSnoozed(taskIds: List<Long>): List<Alarm>

    @Query("SELECT * FROM alarms WHERE task = :taskId")
    suspend fun getAlarms(taskId: Long): List<Alarm>

    @Query("DELETE FROM alarms WHERE _id IN(:alarmIds)")
    suspend fun deleteByIds(alarmIds: List<Long>)

    @Delete
    suspend fun delete(alarm: Alarm)

    @Delete
    suspend fun delete(alarms: List<Alarm>)

    @Insert
    suspend fun insert(alarm: Alarm): Long

    @Insert
    suspend fun insert(alarms: Iterable<Alarm>)

    suspend fun getAlarms(task: Task) = ArrayList(if (task.isNew) {
        emptyList()
    } else {
        getAlarms(task.id)
    })
}