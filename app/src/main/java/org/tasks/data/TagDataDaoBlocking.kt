package org.tasks.data

import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Deprecated("use coroutines")
class TagDataDaoBlocking @Inject constructor(private val dao: TagDataDao) {
    fun tagDataOrderedByName(): List<TagData> = runBlocking {
        dao.tagDataOrderedByName()
    }
}