package org.tasks.etebase

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.CaldavAccount
import org.tasks.ui.CompletableViewModel

class CreateCalendarViewModel @ViewModelInject constructor(
        private val clientProvider: EteBaseClientProvider) : CompletableViewModel<String?>() {
    suspend fun createCalendar(account: CaldavAccount, name: String, color: Int) {
        run { clientProvider.forAccount(account).makeCollection(name, color) }
    }
}