package org.tasks.etebase

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.ui.ActionViewModel

class DeleteCalendarViewModel @ViewModelInject constructor(
        private val clientProvider: EtebaseClientProvider) : ActionViewModel() {
    suspend fun deleteCalendar(account: CaldavAccount, calendar: CaldavCalendar) {
        run { clientProvider.forAccount(account).deleteCollection(calendar) }
    }
}