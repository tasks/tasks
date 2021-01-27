package org.tasks.etesync

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.ui.ActionViewModel
import javax.inject.Inject

@Deprecated("use etebase")
@HiltViewModel
class DeleteCalendarViewModel @Inject constructor(
        private val client: EteSyncClient) : ActionViewModel() {
    suspend fun deleteCalendar(account: CaldavAccount, calendar: CaldavCalendar) {
        run { client.forAccount(account).deleteCollection(calendar) }
    }
}