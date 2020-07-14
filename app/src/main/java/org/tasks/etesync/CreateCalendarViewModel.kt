package org.tasks.etesync

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.CaldavAccount
import org.tasks.ui.CompletableViewModel

class CreateCalendarViewModel @ViewModelInject constructor(
        private val client: EteSyncClient) : CompletableViewModel<String?>() {
    suspend fun createCalendar(account: CaldavAccount, name: String, color: Int) {
        run { client.forAccount(account).makeCollection(name, color) }
    }
}