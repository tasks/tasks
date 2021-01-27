package org.tasks.caldav

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.CaldavAccount
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@HiltViewModel
class CreateCalendarViewModel @Inject constructor(
        private val provider: CaldavClientProvider
): CompletableViewModel<String?>() {
    suspend fun createCalendar(account: CaldavAccount, name: String, color: Int) {
        run { provider.forAccount(account).makeCollection(name, color) }
    }
}