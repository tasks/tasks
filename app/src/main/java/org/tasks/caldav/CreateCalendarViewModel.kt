package org.tasks.caldav

import org.tasks.data.CaldavAccount
import org.tasks.ui.CompletableViewModel

class CreateCalendarViewModel : CompletableViewModel<String?>() {
    fun createCalendar(client: CaldavClient, account: CaldavAccount?, name: String?, color: Int) {
        run { client.forAccount(account).makeCollection(name, color) }
    }
}