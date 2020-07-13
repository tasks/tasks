package org.tasks.etesync

import org.tasks.data.CaldavAccount
import org.tasks.ui.CompletableViewModel

class CreateCalendarViewModel : CompletableViewModel<String?>() {
    fun createCalendar(client: EteSyncClient, account: CaldavAccount?, name: String?, color: Int) {
        run { client.forAccount(account!!).makeCollection(name, color) }
    }
}