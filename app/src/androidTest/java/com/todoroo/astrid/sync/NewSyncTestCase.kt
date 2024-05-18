package com.todoroo.astrid.sync

import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.injection.InjectingTestCase
import javax.inject.Inject

open class NewSyncTestCase : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var tagDataDao: TagDataDao

    suspend fun createTask(): Task {
        val task = Task(
            title = SYNC_TASK_TITLE,
            priority = SYNC_TASK_IMPORTANCE,
        )
        taskDao.createNew(task)
        return task
    }

    suspend fun createTagData(): TagData {
        val tag = TagData(name = "new tag")
        tagDataDao.insert(tag)
        return tag
    }

    companion object {
        private const val SYNC_TASK_TITLE = "new title"
        private const val SYNC_TASK_IMPORTANCE = Task.Priority.MEDIUM
    }
}