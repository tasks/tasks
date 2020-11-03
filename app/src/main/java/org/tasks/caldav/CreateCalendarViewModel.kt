package org.tasks.caldav

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.CaldavAccount
import org.tasks.ui.CompletableViewModel

class CreateCalendarViewModel @ViewModelInject constructor(
        private val provider: CaldavClientProvider
): CompletableViewModel<String?>() {
    suspend fun createCalendar(account: CaldavAccount, name: String, color: Int) {
        run { provider.forAccount(account).makeCollection(name, color) }
    }
}