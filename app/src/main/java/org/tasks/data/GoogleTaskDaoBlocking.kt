package org.tasks.data

import javax.inject.Inject

@Deprecated("use coroutines")
class GoogleTaskDaoBlocking @Inject constructor(private val dao: GoogleTaskDao) {
    fun setCollapsed(id: Long, collapsed: Boolean) = runBlocking {
        dao.setCollapsed(id, collapsed)
    }
}