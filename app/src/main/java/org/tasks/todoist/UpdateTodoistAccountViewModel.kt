package org.tasks.todoist

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.Strings.isNullOrEmpty
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@HiltViewModel
class UpdateTodoistAccountViewModel @Inject constructor(
        private val clientProvider: TodoistClientProvider) : CompletableViewModel<String>() {
    suspend fun updateAccount(url: String, user: String, pass: String?, session: String) {
        run {
            if (isNullOrEmpty(pass)) {
                clientProvider.forUrl(url, user, null, session, true).getSession()
            } else {
                clientProvider
                        .forUrl(url, user, pass, foreground = true)
                        .getSession()
            }
        }
    }
}
