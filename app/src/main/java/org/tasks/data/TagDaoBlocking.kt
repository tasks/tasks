package org.tasks.data

import com.todoroo.astrid.data.Task
import javax.inject.Inject

@Deprecated("use coroutines")
class TagDaoBlocking @Inject constructor(private val dao: TagDao) {
    fun rename(tagUid: String, name: String) = runBlocking {
        dao.rename(tagUid, name)
    }

    fun insert(tag: Tag) = runBlocking {
        dao.insert(tag)
    }

    fun insert(tags: Iterable<Tag>) = runBlocking {
        dao.insert(tags)
    }

    fun deleteTags(taskId: Long, tagUids: List<String>) = runBlocking {
        dao.deleteTags(taskId, tagUids)
    }

    fun getByTagUid(tagUid: String): List<Tag> = runBlocking {
        dao.getByTagUid(tagUid)
    }

    fun getTagsForTask(taskId: Long): List<Tag> = runBlocking {
        dao.getTagsForTask(taskId)
    }

    fun getTagByTaskAndTagUid(taskId: Long, tagUid: String): Tag? = runBlocking {
        dao.getTagByTaskAndTagUid(taskId, tagUid)
    }

    fun delete(tags: List<Tag>) = runBlocking {
        dao.delete(tags)
    }

    fun applyTags(task: Task, tagDataDao: TagDataDaoBlocking, current: List<TagData>): Boolean = runBlocking {
        dao.applyTags(task, tagDataDao.dao, current)
    }

    fun insert(task: Task, tags: Collection<TagData>) = runBlocking {
        dao.insert(task, tags)
    }
}