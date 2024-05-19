package org.tasks.data

import androidx.room.Embedded
import org.tasks.data.entity.Task
import org.tasks.data.dao.CaldavDao.Companion.toAppleEpoch
import org.tasks.data.entity.CaldavTask

class CaldavTaskContainer {
    @Embedded lateinit var task: Task
    @Embedded lateinit var caldavTask: CaldavTask

    val id: Long
        get() = task.id

    val remoteId: String?
        get() = caldavTask.remoteId

    val isDeleted: Boolean
        get() = task.isDeleted

    val sortOrder: Long
        get() = task.order ?: task.creationDate.toAppleEpoch()

    val startDate: Long
        get() = task.hideUntil

    override fun toString(): String = "CaldavTaskContainer{task=$task, caldavTask=$caldavTask}"
}