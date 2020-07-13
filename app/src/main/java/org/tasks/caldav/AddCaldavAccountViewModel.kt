package org.tasks.caldav

import org.tasks.ui.CompletableViewModel

class AddCaldavAccountViewModel : CompletableViewModel<String>() {
    fun addAccount(client: CaldavClient, url: String?, username: String?, password: String?) {
        run { client.setForeground().forUrl(url, username, password).homeSet }
    }
}