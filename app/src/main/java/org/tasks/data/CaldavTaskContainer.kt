package org.tasks.data

import androidx.room.Embedded
import com.todoroo.astrid.data.Task

class CaldavTaskContainer {
    @Embedded lateinit var task: Task
    @Embedded lateinit var caldavTask: CaldavTask

    val remoteId: String?
        get() = caldavTask.remoteId

    val isDeleted: Boolean
        get() = task.isDeleted

    val vtodo: String?
        get() = caldavTask.vtodo

    override fun toString(): String {
        return "CaldavTaskContainer{task=$task, caldavTask=$caldavTask}"
    }
}