package org.tasks.caldav

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@HiltViewModel
class AddCaldavAccountViewModel @Inject constructor(
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