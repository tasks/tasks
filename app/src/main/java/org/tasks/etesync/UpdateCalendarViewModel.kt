package org.tasks.etesync

import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.ui.CompletableViewModel

class UpdateCalendarViewModel : CompletableViewModel<String?>() {
    fun updateCalendar(
            client: EteSyncClient,
            account: CaldavAccount?,
            calendar: CaldavCalendar?,
            name: String?,
            color: Int) {
        run { client.forAccount(account!!).updateCollection(calendar!!, name, color) }
    }
}