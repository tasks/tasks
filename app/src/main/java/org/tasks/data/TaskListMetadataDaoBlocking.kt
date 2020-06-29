package org.tasks.data

import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Deprecated("use coroutines")
class TaskListMetadataDaoBlocking @Inject constructor(private val dao: TaskListMetadataDao) {
    fun fetchByTagOrFilter(tagUuid: String): TaskListMetadata? = runBlocking {
        dao.fetchByTagOrFilter(tagUuid)
    }

    fun getAll(): List<TaskListMetadata> = runBlocking {
        dao.getAll()
    }

    fun update(taskListMetadata: TaskListMetadata) = runBlocking {
        dao.update(taskListMetadata)
    }

    fun insert(taskListMetadata: TaskListMetadata): Long = runBlocking {
        dao.insert(taskListMetadata)
    }

    fun createNew(taskListMetadata: TaskListMetadata) = runBlocking {
        dao.createNew(taskListMetadata)
    }
}