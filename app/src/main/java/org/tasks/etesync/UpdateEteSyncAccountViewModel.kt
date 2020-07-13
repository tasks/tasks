package org.tasks.etesync

import androidx.core.util.Pair
import com.etesync.journalmanager.UserInfoManager
import org.tasks.Strings.isNullOrEmpty
import org.tasks.ui.CompletableViewModel

class UpdateEteSyncAccountViewModel : CompletableViewModel<Pair<UserInfoManager.UserInfo, String>>() {
    fun updateAccount(
            client: EteSyncClient, url: String?, user: String?, pass: String?, token: String?) {
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