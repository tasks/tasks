package org.tasks.data

import androidx.lifecycle.LiveData
import javax.inject.Inject

@Deprecated("use coroutines")
class TagDataDaoBlocking @Inject constructor(private val dao: TagDataDao) {
    fun subscribeToTags(): LiveData<List<TagData>> {
        return dao.subscribeToTags()
    }

    fun searchTags(query: String): List<TagData> = runBlocking {
        dao.searchTags(query)
    }

    fun tagDataOrderedByName(): List<TagData> = runBlocking {
        dao.tagDataOrderedByName()
    }

    fun createNew(tag: TagData) = runBlocking {
        dao.createNew(tag)
    }
}