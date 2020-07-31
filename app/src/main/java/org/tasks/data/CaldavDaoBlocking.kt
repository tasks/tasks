package org.tasks.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Deprecated("use coroutines")
class CaldavDaoBlocking @Inject constructor(private val dao: CaldavDao) {
    fun subscribeToCalendars(): LiveData<List<CaldavCalendar>> {
        return dao.subscribeToCalendars()
    }

    fun getCalendars(): List<CaldavCalendar> = runBlocking {
        dao.getCalendars()
    }
}