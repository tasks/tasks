package org.tasks.data

import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Deprecated("use coroutines")
class CaldavDaoBlocking @Inject constructor(private val dao: CaldavDao) {
    fun getCalendars(): List<CaldavCalendar> = runBlocking {
        dao.getCalendars()
    }
}