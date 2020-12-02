package org.tasks.etebase

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.Strings.isNullOrEmpty
import org.tasks.ui.CompletableViewModel

class UpdateEtebaseAccountViewModel @ViewModelInject constructor(
        private val clientProvider: EtebaseClientProvider) : CompletableViewModel<String>() {
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