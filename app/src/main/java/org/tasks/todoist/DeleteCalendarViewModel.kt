package org.tasks.todoist

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.ui.ActionViewModel
import javax.inject.Inject

@HiltViewModel
class DeleteCalendarViewModel @Inject constructor(
        private val clientProvider: TodoistClientProvider) : ActionViewModel() {
    suspend fun deleteCalendar(account: CaldavAccount, calendar: CaldavCalendar) {
        run { clientProvider.forAccount(account).deleteCollection(calendar) }
    }
}
