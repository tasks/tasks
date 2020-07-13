package org.tasks.caldav

import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.ui.CompletableViewModel

class UpdateCalendarViewModel : CompletableViewModel<String?>() {
    fun updateCalendar(
            client: CaldavClient, account: CaldavAccount?, calendar: CaldavCalendar?, name: String?, color: Int) {
        run { client.forCalendar(account, calendar).updateCollection(name, color) }
    }
}