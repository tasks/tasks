package org.tasks.data

import javax.inject.Inject

@Deprecated("use coroutines")
class TagDaoBlocking @Inject constructor(private val dao: TagDao) {
    fun insert(tag: Tag) = runBlocking {
        dao.insert(tag)
    }

    fun insert(tags: Iterable<Tag>) = runBlocking {
        dao.insert(tags)
    }
}