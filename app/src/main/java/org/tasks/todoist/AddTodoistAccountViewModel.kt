package org.tasks.todoist

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@HiltViewModel
class AddTodoistAccountViewModel @Inject constructor(
        private val clientProvider: TodoistClientProvider): CompletableViewModel<String>() {
    suspend fun addAccount(url: String, username: String, password: String) {
        run {
            clientProvider
                    .forUrl(url, username, password, foreground = true)
                    .getSession()
        }
    }
}
