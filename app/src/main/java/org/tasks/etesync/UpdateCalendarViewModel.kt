package org.tasks.etesync

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.ui.CompletableViewModel

class UpdateCalendarViewModel @ViewModelInject constructor(
        private val client: EteSyncClient): CompletableViewModel<String?>() {
    suspend fun updateCalendar(account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) {
        run { client.forAccount(account).updateCollection(calendar, name, color) }
    }
}