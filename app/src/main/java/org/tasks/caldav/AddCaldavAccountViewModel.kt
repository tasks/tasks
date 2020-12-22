package org.tasks.caldav

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.ui.CompletableViewModel

class AddCaldavAccountViewModel @ViewModelInject constructor(
        private val provider: CaldavClientProvider
) : CompletableViewModel<String>() {
    suspend fun addAccount(url: String, username: String, password: String) {
        run {
            provider
                    .forUrl(url, username, password)
                    .setForeground()
                    .homeSet(username, password)
        }
    }
}