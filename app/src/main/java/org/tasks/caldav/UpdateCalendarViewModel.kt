package org.tasks.caldav

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.ui.CompletableViewModel

class UpdateCalendarViewModel @ViewModelInject constructor(
        private val provider: CaldavClientProvider
) : CompletableViewModel<String?>() {
    suspend fun updateCalendar(account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) {
        run {
            calendar.url?.let { provider.forAccount(account, it).updateCollection(name, color) }
        }
    }
}