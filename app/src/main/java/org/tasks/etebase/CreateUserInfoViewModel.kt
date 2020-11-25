package org.tasks.etebase

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.CaldavAccount
import org.tasks.ui.CompletableViewModel

class CreateUserInfoViewModel @ViewModelInject constructor(
        private val clientProvider: EteBaseClientProvider): CompletableViewModel<String>() {
    suspend fun createUserInfo(caldavAccount: CaldavAccount, derivedKey: String) {
        run {
            clientProvider.forAccount(caldavAccount).createUserInfo(derivedKey)
            derivedKey
        }
    }
}