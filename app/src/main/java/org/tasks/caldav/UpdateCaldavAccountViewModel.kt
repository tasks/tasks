package org.tasks.caldav

import org.tasks.ui.CompletableViewModel

class UpdateCaldavAccountViewModel : CompletableViewModel<String>() {
    fun updateCaldavAccount(
            client: CaldavClient, url: String?, username: String?, password: String?) {
        run {
            client.forUrl(url, username, password).homeSet
        }
    }
}