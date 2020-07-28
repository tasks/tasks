package org.tasks.data

import androidx.lifecycle.LiveData
import javax.inject.Inject

@Deprecated("use coroutines")
class CaldavDaoBlocking @Inject constructor(private val dao: CaldavDao) {
    fun subscribeToCalendars(): LiveData<List<CaldavCalendar>> {
        return dao.subscribeToCalendars()
    }

    fun setCollapsed(id: Long, collapsed: Boolean) = runBlocking {
        dao.setCollapsed(id, collapsed)
    }

    fun getCalendars(): List<CaldavCalendar> = runBlocking {
        dao.getCalendars()
    }
}