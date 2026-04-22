package org.tasks.etebase

import com.etebase.client.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.OkHttpClientFactory
import org.tasks.security.KeyStoreEncryption
import java.util.concurrent.TimeUnit

class EtebaseClientProvider(
    private val filesDir: String,
    private val encryption: KeyStoreEncryption,
    private val caldavDao: CaldavDao,
    private val httpClientFactory: OkHttpClientFactory,
) {
    suspend fun forAccount(account: CaldavAccount): EtebaseClient = forUrl(
        account.url!!,
        account.username!!,
        null,
        encryption.decrypt(account.password) ?: "",
    )

    suspend fun forUrl(
        url: String,
        username: String,
        password: String?,
        session: String? = null,
        foreground: Boolean = false,
    ): EtebaseClient = withContext(Dispatchers.IO) {
        val okHttpClient = httpClientFactory.newClient(
            foreground = foreground,
            cookieKey = username,
        ) { builder ->
            builder
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
        }
        val client = OkHttpBridge.createClient(okHttpClient, url)
        val etebase = session
            ?.let { Account.restore(client, it, null) }
            ?: Account.login(client, username, password!!)
        EtebaseClient(filesDir, username, etebase, caldavDao)
    }
}
