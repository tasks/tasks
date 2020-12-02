package org.tasks.etebase

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.ui.CompletableViewModel

class AddEtebaseAccountViewModel @ViewModelInject constructor(
        private val clientProvider: EtebaseClientProvider): CompletableViewModel<String>() {
    suspend fun addAccount(url: String, username: String, password: String) {
        run {
            clientProvider
                    .forUrl(url, username, password, foreground = true)
                    .getSession()
        }
    }
}