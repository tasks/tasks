package org.tasks.etesync

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.CaldavAccount
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@Deprecated("use etebase")
@HiltViewModel
class CreateCalendarViewModel @Inject constructor(
        private val client: EteSyncClient) : CompletableViewModel<String?>() {
    suspend fun createCalendar(account: CaldavAccount, name: String, color: Int) {
        run { client.forAccount(account).makeCollection(name, color) }
    }
}