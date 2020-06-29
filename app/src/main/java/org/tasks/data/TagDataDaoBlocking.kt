package org.tasks.data

import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.runBlocking
import org.tasks.filters.TagFilters
import org.tasks.time.DateTimeUtils.currentTimeMillis
import javax.inject.Inject

@Deprecated("use coroutines")
class TagDataDaoBlocking @Inject constructor(private val dao: TagDataDao) {
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

    fun getByUuid(uuids: Collection<String>): List<TagData> = runBlocking {
        dao.getByUuid(uuids)
    }

    fun tagDataOrderedByName(): List<TagData> = runBlocking {
        dao.tagDataOrderedByName()
    }

    fun deleteTagData(tagData: TagData) = runBlocking {
        dao.deleteTagData(tagData)
    }

    fun deleteTags(tagUid: String) = runBlocking {
        dao.deleteTags(tagUid)
    }

    fun tagsToDelete(tasks: List<Long>, tagsToKeep: List<String>): List<Tag> = runBlocking {
        dao.tagsToDelete(tasks, tagsToKeep)
    }

    fun getTagSelections(tasks: List<Long>): Pair<Set<String>, Set<String>> = runBlocking {
        dao.getTagSelections(tasks)
    }

    fun getAllTags(tasks: List<Long>): List<String> = runBlocking {
        dao.getAllTags(tasks)
    }

    fun applyTags(tasks: List<Task>, partiallySelected: List<TagData>, selected: List<TagData>): List<Long> = runBlocking {
        dao.applyTags(tasks, partiallySelected, selected)
    }

    fun delete(tagData: TagData) = runBlocking {
        dao.delete(tagData)
    }

    fun delete(tagData: List<TagData>) = runBlocking {
        dao.delete(tagData)
    }

    fun deleteTags(tags: List<Tag>) = runBlocking {
        dao.deleteTags(tags)
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

    fun insert(tag: TagData): Long = runBlocking {
        dao.insert(tag)
    }

    fun insert(tags: Iterable<Tag>) = runBlocking {
        dao.insert(tags)
    }

    fun createNew(tag: TagData) = runBlocking {
        dao.createNew(tag)
    }

    fun getTagFilters(now: Long = currentTimeMillis()): List<TagFilters> = runBlocking {
        dao.getTagFilters(now)
    }

    fun resetOrders() = runBlocking {
        dao.resetOrders()
    }

    fun setOrder(id: Long, order: Int) = runBlocking {
        dao.setOrder(id, order)
    }
}