package org.tasks.etebase

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.ui.CompletableViewModel

class AddEteBaseAccountViewModel @ViewModelInject constructor(
        private val clientProvider: EteBaseClientProvider): CompletableViewModel<String>() {
    suspend fun addAccount(url: String, username: String, password: String) {
        run {
            clientProvider
                    .forUrl(url, username, password, foreground = true)
                    .getSession()
        }
    }
}