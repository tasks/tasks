package org.tasks.todoist

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.entity.CaldavAccount
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@HiltViewModel
class CreateCalendarViewModel @Inject constructor(
        private val clientProvider: TodoistClientProvider) : CompletableViewModel<String?>() {
    suspend fun createCalendar(account: CaldavAccount, name: String, color: Int) {
        run { clientProvider.forAccount(account).makeCollection(name, color) }
    }
}
