package org.tasks.jobs

import org.tasks.data.entity.Notification
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.printTimestamp

data class AlarmEntry(
    val id: Long,
    val taskId: Long,
    val time: Long,
    val type: Int
) {
    fun toNotification(): Notification = Notification(
        taskId = taskId,
        type = type,
        timestamp = currentTimeMillis(),
    )

    override fun toString(): String {
        return "AlarmEntry(id=$id, taskId=$taskId, time=${printTimestamp(time)}, type=$type)"
    }
}
