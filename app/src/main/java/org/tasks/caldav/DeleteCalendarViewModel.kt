package org.tasks.caldav

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.ui.ActionViewModel
import javax.inject.Inject

@HiltViewModel
class DeleteCalendarViewModel @Inject constructor(
        private val provider: CaldavClientProvider
) : ActionViewModel() {
    suspend fun deleteCalendar(account: CaldavAccount, calendar: CaldavCalendar) {
        run {
            calendar.url?.let { provider.forAccount(account, it).deleteCollection() }
        }
    }
}