package org.tasks.etebase

import androidx.core.util.Pair
import androidx.hilt.lifecycle.ViewModelInject
import com.etesync.journalmanager.UserInfoManager.UserInfo
import org.tasks.ui.CompletableViewModel

class AddEteBaseAccountViewModel @ViewModelInject constructor(
        private val client: EteBaseClient): CompletableViewModel<Pair<UserInfo, String>>() {
    suspend fun addAccount(url: String, username: String, password: String) {
        run {
            client.setForeground()
            val token = client.forUrl(url, username, null, null).getToken(password)
            Pair.create(client.forUrl(url, username, null, token!!).userInfo(), token)
        }
    }
}