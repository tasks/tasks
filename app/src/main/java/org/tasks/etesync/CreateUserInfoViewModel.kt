package org.tasks.etesync

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.CaldavAccount
import org.tasks.ui.CompletableViewModel

class CreateUserInfoViewModel @ViewModelInject constructor(
        private val client: EteSyncClient): CompletableViewModel<String>() {
    fun createUserInfo(caldavAccount: CaldavAccount, derivedKey: String) {
        run {
            client.forAccount(caldavAccount).createUserInfo(derivedKey)
            derivedKey
        }
    }
}