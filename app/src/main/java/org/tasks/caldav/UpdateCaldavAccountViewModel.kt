package org.tasks.caldav

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.ui.CompletableViewModel

class UpdateCaldavAccountViewModel @ViewModelInject constructor(
        private val provider: CaldavClientProvider
) : CompletableViewModel<String>() {
    suspend fun updateCaldavAccount(url: String, username: String, password: String) {
        run { provider.forUrl(url, username, password).homeSet(username, password) }
    }

    override fun onCleared() {
        provider.dispose()
    }
}