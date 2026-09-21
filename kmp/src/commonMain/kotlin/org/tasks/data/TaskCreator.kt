package org.tasks.data

import org.tasks.data.entity.Task
import org.tasks.time.DateTimeUtils2.currentTimeMillis

class TaskCreator {
    fun createBlankTask(
        title: String? = null,
        remoteId: String = UUIDHelper.newUUID(),
    ): Task {
        val now = currentTimeMillis()
        return Task(
            title = title?.trim(),
            creationDate = now,
            modificationDate = now,
            remoteId = remoteId,
        )
    }
}
