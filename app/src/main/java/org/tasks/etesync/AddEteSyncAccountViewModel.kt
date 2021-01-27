package org.tasks.etesync

import androidx.core.util.Pair
import com.etesync.journalmanager.UserInfoManager.UserInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@Deprecated("use etebase")
@HiltViewModel
class AddEteSyncAccountViewModel @Inject constructor(
        private val client: EteSyncClient): CompletableViewModel<Pair<UserInfo, String>>() {
    suspend fun addAccount(url: String, username: String, password: String) {
        run {
            client.setForeground()
            val token = client.forUrl(url, username, null, null).getToken(password)
            Pair.create(client.forUrl(url, username, null, token!!).userInfo(), token)
        }
    }
}