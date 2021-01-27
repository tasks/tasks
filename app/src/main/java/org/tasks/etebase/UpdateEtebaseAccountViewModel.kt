package org.tasks.etebase

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.Strings.isNullOrEmpty
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@HiltViewModel
class UpdateEtebaseAccountViewModel @Inject constructor(
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