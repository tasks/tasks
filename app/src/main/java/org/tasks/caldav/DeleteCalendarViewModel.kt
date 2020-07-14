package org.tasks.caldav

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.ui.ActionViewModel

class DeleteCalendarViewModel @ViewModelInject constructor(
        private val client: CaldavClient) : ActionViewModel() {
    suspend fun deleteCalendar(account: CaldavAccount, calendar: CaldavCalendar) {
        run { client.forCalendar(account, calendar).deleteCollection() }
    }
}