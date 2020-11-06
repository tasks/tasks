package org.tasks.auth

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.CaldavDao
import org.tasks.ui.CompletableViewModel

class SignInViewModel @ViewModelInject constructor(
        @ApplicationContext private val context: Context,
        private val provider: CaldavClientProvider,
        private val caldavDao: CaldavDao
) : CompletableViewModel<CaldavAccount?>() {

    suspend fun validate(id: String, email: String, idToken: String) {
        run {
            val homeSet = provider
                    .forUrl(
                            "${context.getString(R.string.tasks_caldav_url)}/google_login",
                            token = idToken
                    )
                    .setForeground()
                    .homeSet(token = idToken)
            val username = "google_$id"
            caldavDao.getAccount(TYPE_TASKS, username)
                    ?.apply {
                        error = null
                        caldavDao.update(this)
                    }
                    ?: CaldavAccount().apply {
                        accountType = TYPE_TASKS
                        uuid = UUIDHelper.newUUID()
                        url = homeSet
                        this.username = username
                        name = email
                        caldavDao.insert(this)
                    }
        }
    }
}
