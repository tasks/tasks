package org.tasks.data

import javax.inject.Inject

@Deprecated("use coroutines")
class TaskAttachmentDaoBlocking @Inject constructor(private val dao: TaskAttachmentDao) {
    fun getAttachments(taskUuid: String): List<TaskAttachment> = runBlocking {
        dao.getAttachments(taskUuid)
    }

    fun getAttachments(task: Long): List<TaskAttachment> = runBlocking {
        dao.getAttachments(task)
    }

    fun getAttachments(): List<TaskAttachment> = runBlocking {
        dao.getAttachments()
    }

    fun delete(taskAttachment: TaskAttachment) = runBlocking {
        dao.delete(taskAttachment)
    }

    fun insert(attachment: TaskAttachment) = runBlocking {
        dao.insert(attachment)
    }

    fun update(attachment: TaskAttachment) = runBlocking {
        dao.update(attachment)
    }

    fun createNew(attachment: TaskAttachment) = runBlocking {
        dao.createNew(attachment)
    }
}