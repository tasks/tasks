package org.tasks.data

import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import javax.inject.Inject

@Deprecated("use coroutines")
class TagDataDaoBlocking @Inject constructor(val dao: TagDataDao) {
    fun subscribeToTags(): LiveData<List<TagData>> {
        return dao.subscribeToTags()
    }

    fun getTagByName(name: String): TagData? = runBlocking {
        dao.getTagByName(name)
    }

    fun getTagWithCase(tag: String): String? = runBlocking {
        dao.getTagWithCase(tag)
    }

    fun searchTags(query: String): List<TagData> = runBlocking {
        dao.searchTags(query)
    }

    fun getAll(): List<TagData> = runBlocking {
        dao.getAll()
    }

    fun getByUuid(uuid: String): TagData? = runBlocking {
        dao.getByUuid(uuid)
    }

    fun tagDataOrderedByName(): List<TagData> = runBlocking {
        dao.tagDataOrderedByName()
    }

    fun getTagSelections(tasks: List<Long>): Pair<Set<String>, Set<String>> = runBlocking {
        dao.getTagSelections(tasks)
    }

    fun delete(tagData: TagData) = runBlocking {
        dao.delete(tagData)
    }

    fun delete(tagData: List<TagData>) = runBlocking {
        dao.delete(tagData)
    }

    fun getTagDataForTask(id: Long): List<TagData> = runBlocking {
        dao.getTagDataForTask(id)
    }

    fun getTags(names: List<String>): List<TagData> = runBlocking {
        dao.getTags(names)
    }

    fun update(tagData: TagData) = runBlocking {
        dao.update(tagData)
    }

    fun createNew(tag: TagData) = runBlocking {
        dao.createNew(tag)
    }

    fun resetOrders() = runBlocking {
        dao.resetOrders()
    }

    fun setOrder(id: Long, order: Int) = runBlocking {
        dao.setOrder(id, order)
    }
}