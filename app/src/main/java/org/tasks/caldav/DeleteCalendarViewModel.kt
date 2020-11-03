package org.tasks.caldav

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.ui.ActionViewModel

class DeleteCalendarViewModel @ViewModelInject constructor(
        private val provider: CaldavClientProvider
) : ActionViewModel() {
    suspend fun deleteCalendar(account: CaldavAccount, calendar: CaldavCalendar) {
        run {
            calendar.url?.let { provider.forAccount(account, it).deleteCollection() }
        }
    }
}