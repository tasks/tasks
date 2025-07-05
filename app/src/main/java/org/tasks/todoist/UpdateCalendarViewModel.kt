package org.tasks.todoist

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@HiltViewModel
class UpdateCalendarViewModel @Inject constructor(
        private val clientProvider: TodoistClientProvider): CompletableViewModel<String?>() {
    suspend fun updateCalendar(account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) {
        run { clientProvider.forAccount(account).updateCollection(calendar, name, color) }
    }
}
