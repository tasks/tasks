package org.tasks.data

import javax.inject.Inject

@Deprecated("use coroutines")
class TaskListMetadataDaoBlocking @Inject constructor(private val dao: TaskListMetadataDao) {
    fun fetchByTagOrFilter(tagUuid: String): TaskListMetadata? = runBlocking {
        dao.fetchByTagOrFilter(tagUuid)
    }

    fun getAll(): List<TaskListMetadata> = runBlocking {
        dao.getAll()
    }

    fun insert(taskListMetadata: TaskListMetadata): Long = runBlocking {
        dao.insert(taskListMetadata)
    }
}