package org.tasks.etesync

import org.tasks.data.CaldavAccount
import org.tasks.ui.CompletableViewModel

class CreateUserInfoViewModel : CompletableViewModel<String?>() {
    fun createUserInfo(client: EteSyncClient, caldavAccount: CaldavAccount?, derivedKey: String?) {
        run {
            client.forAccount(caldavAccount!!).createUserInfo(derivedKey)
            derivedKey
        }
    }
}