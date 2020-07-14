package org.tasks.caldav

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.CaldavAccount
import org.tasks.ui.CompletableViewModel

class CreateCalendarViewModel @ViewModelInject constructor(
        private val client: CaldavClient): CompletableViewModel<String?>() {
    fun createCalendar(account: CaldavAccount, name: String, color: Int) {
        run { client.forAccount(account).makeCollection(name, color) }
    }
}