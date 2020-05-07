package com.todoroo.astrid.sync

import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import javax.inject.Inject

open class NewSyncTestCase : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var tagDataDao: TagDataDao

    fun createTask(): Task {
        val task = Task()
        task.title = SYNC_TASK_TITLE
        task.priority = SYNC_TASK_IMPORTANCE
        taskDao.createNew(task)
        return task
    }

    fun createTagData(): TagData {
        val tag = TagData()
        tag.name = "new tag"
        tagDataDao.createNew(tag)
        return tag
    }

    override fun inject(component: TestComponent) = component.inject(this)

    companion object {
        private const val SYNC_TASK_TITLE = "new title"
        private const val SYNC_TASK_IMPORTANCE = Task.Priority.MEDIUM
    }
}