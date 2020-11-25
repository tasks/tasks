package org.tasks.etebase

import androidx.core.util.Pair
import androidx.hilt.lifecycle.ViewModelInject
import com.etesync.journalmanager.UserInfoManager.UserInfo
import org.tasks.ui.CompletableViewModel

class AddEteBaseAccountViewModel @ViewModelInject constructor(
        private val clientProvider: EteBaseClientProvider): CompletableViewModel<Pair<UserInfo, String>>() {
    suspend fun addAccount(url: String, username: String, password: String) {
        run {
            val token =
                    clientProvider
                            .forUrl(url, username, null, null)
                            .setForeground()
                            .getToken(password)
            Pair.create(
                    clientProvider.forUrl(url, username, null, token!!).userInfo(),
                    token
            )
        }
    }
}