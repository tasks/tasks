package org.tasks.etebase

import androidx.core.util.Pair
import androidx.hilt.lifecycle.ViewModelInject
import com.etesync.journalmanager.UserInfoManager.UserInfo
import org.tasks.Strings.isNullOrEmpty
import org.tasks.ui.CompletableViewModel

class UpdateEteBaseAccountViewModel @ViewModelInject constructor(
        private val clientProvider: EteBaseClientProvider) : CompletableViewModel<Pair<UserInfo, String>>() {
    suspend fun updateAccount(url: String, user: String, pass: String?, token: String) {
        run {
            if (isNullOrEmpty(pass)) {
                Pair.create(
                        clientProvider.forUrl(url, user, null, token).setForeground().userInfo(),
                        token
                )
            } else {
                val newToken =
                        clientProvider
                                .forUrl(url, user, null, null)
                                .setForeground()
                                .getToken(pass)!!
                Pair.create(
                        clientProvider.forUrl(url, user, null, newToken).userInfo(),
                        newToken
                )
            }
        }
    }
}