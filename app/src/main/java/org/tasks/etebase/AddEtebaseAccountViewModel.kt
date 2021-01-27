package org.tasks.etebase

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@HiltViewModel
class AddEtebaseAccountViewModel @Inject constructor(
        private val clientProvider: EtebaseClientProvider): CompletableViewModel<String>() {
    suspend fun addAccount(url: String, username: String, password: String) {
        run {
            clientProvider
                    .forUrl(url, username, password, foreground = true)
                    .getSession()
        }
    }
}