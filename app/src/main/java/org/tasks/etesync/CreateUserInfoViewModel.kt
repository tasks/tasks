package org.tasks.etesync

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.CaldavAccount
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@Deprecated("use etebase")
@HiltViewModel
class CreateUserInfoViewModel @Inject constructor(
        private val client: EteSyncClient): CompletableViewModel<String>() {
    suspend fun createUserInfo(caldavAccount: CaldavAccount, derivedKey: String) {
        run {
            client.forAccount(caldavAccount).createUserInfo(derivedKey)
            derivedKey
        }
    }
}