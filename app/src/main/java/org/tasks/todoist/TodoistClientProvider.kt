package org.tasks.todoist

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.dao.CaldavDao
import org.tasks.data.getPassword
import org.tasks.http.HttpClientFactory
import org.tasks.security.KeyStoreEncryption
import javax.inject.Inject

class TodoistClientProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryption: KeyStoreEncryption,
    private val caldavDao: CaldavDao,
    private val httpClientFactory: HttpClientFactory,
) {
    suspend fun forAccount(account: CaldavAccount): TodoistClient = forUrl(
            account.url!!,
            account.username!!,
            null,
            account.getPassword(encryption))

    suspend fun forUrl(url: String, username: String, password: String?, session: String? = null, foreground: Boolean = false): TodoistClient = withContext(Dispatchers.IO) {
        // Stubbed implementation for Todoist API
        TodoistClient(context, username, caldavDao)
    }
}
