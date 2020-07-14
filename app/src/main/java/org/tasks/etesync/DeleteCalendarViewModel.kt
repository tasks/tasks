package org.tasks.etesync

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.ui.ActionViewModel

class DeleteCalendarViewModel @ViewModelInject constructor(
        private val client: EteSyncClient) : ActionViewModel() {
    suspend fun deleteCalendar(account: CaldavAccount, calendar: CaldavCalendar) {
        run { client.forAccount(account).deleteCollection(calendar) }
    }
}