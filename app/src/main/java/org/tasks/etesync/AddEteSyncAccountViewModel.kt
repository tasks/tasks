package org.tasks.etesync

import androidx.core.util.Pair
import com.etesync.journalmanager.UserInfoManager
import org.tasks.ui.CompletableViewModel

class AddEteSyncAccountViewModel : CompletableViewModel<Pair<UserInfoManager.UserInfo, String>>() {
    fun addAccount(client: EteSyncClient, url: String?, username: String?, password: String?) {
        run {
            client.setForeground()
            val token = client.forUrl(url, username, null, null).getToken(password)
            Pair.create(client.forUrl(url, username, null, token!!).userInfo, token)
        }
    }
}