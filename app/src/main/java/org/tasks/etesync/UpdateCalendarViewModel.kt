package org.tasks.etesync

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@Deprecated("use etebase")
@HiltViewModel
class UpdateCalendarViewModel @Inject constructor(
        private val client: EteSyncClient): CompletableViewModel<String?>() {
    suspend fun updateCalendar(account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) {
        run { client.forAccount(account).updateCollection(calendar, name, color) }
    }
}