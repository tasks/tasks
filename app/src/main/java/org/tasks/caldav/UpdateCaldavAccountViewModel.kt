package org.tasks.caldav

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.ui.CompletableViewModel

class UpdateCaldavAccountViewModel @ViewModelInject constructor(
        private val client: CaldavClient) : CompletableViewModel<String>() {
    fun updateCaldavAccount(url: String, username: String, password: String?) {
        run { client.forUrl(url, username, password).homeSet }
    }
}