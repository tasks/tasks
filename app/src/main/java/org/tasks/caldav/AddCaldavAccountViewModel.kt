package org.tasks.caldav

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.ui.CompletableViewModel

class AddCaldavAccountViewModel @ViewModelInject constructor(
        private val client: CaldavClient) : CompletableViewModel<String>() {
    fun addAccount(url: String, username: String, password: String) {
        run { client.setForeground().forUrl(url, username, password).homeSet }
    }
}