package org.tasks.etebase

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.ui.ActionViewModel
import javax.inject.Inject

@HiltViewModel
class DeleteCalendarViewModel @Inject constructor(
        private val clientProvider: EtebaseClientProvider) : ActionViewModel() {
    suspend fun deleteCalendar(account: CaldavAccount, calendar: CaldavCalendar) {
        run { clientProvider.forAccount(account).deleteCollection(calendar) }
    }
}