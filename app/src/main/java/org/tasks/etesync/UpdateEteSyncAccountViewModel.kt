package org.tasks.etesync

import androidx.core.util.Pair
import com.etesync.journalmanager.UserInfoManager.UserInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.Strings.isNullOrEmpty
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@Deprecated("use etebase")
@HiltViewModel
class UpdateEteSyncAccountViewModel @Inject constructor(
        private val client: EteSyncClient) : CompletableViewModel<Pair<UserInfo, String>>() {
    suspend fun updateAccount(url: String, user: String, pass: String?, token: String) {
        run {
            client.setForeground()
            if (isNullOrEmpty(pass)) {
                Pair.create(client.forUrl(url, user, null, token).userInfo(), token)
            } else {
                val newToken = client.forUrl(url, user, null, null).getToken(pass)
                Pair.create(client.forUrl(url, user, null, newToken).userInfo(), newToken)
            }
        }
    }
}