package org.tasks.etesync

import androidx.core.util.Pair
import androidx.hilt.lifecycle.ViewModelInject
import com.etesync.journalmanager.UserInfoManager.UserInfo
import org.tasks.Strings.isNullOrEmpty
import org.tasks.ui.CompletableViewModel

class UpdateEteSyncAccountViewModel @ViewModelInject constructor(
        private val client: EteSyncClient) : CompletableViewModel<Pair<UserInfo, String>>() {
    fun updateAccount(url: String, user: String, pass: String?, token: String) {
        run {
            client.setForeground()
            if (isNullOrEmpty(pass)) {
                Pair.create(client.forUrl(url, user, null, token).userInfo, token)
            } else {
                val newToken = client.forUrl(url, user, null, null).getToken(pass)
                Pair.create(client.forUrl(url, user, null, newToken).userInfo, newToken)
            }
        }
    }
}