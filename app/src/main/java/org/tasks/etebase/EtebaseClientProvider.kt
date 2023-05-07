package org.tasks.etebase

import android.content.Context
import com.etebase.client.Account
import com.etebase.client.Client
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavDao
import org.tasks.http.HttpClientFactory
import org.tasks.security.KeyStoreEncryption
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class EtebaseClientProvider @Inject constructor(
        @ApplicationContext private val context: Context,
        private val encryption: KeyStoreEncryption,
        private val caldavDao: CaldavDao,
        private val httpClientFactory: HttpClientFactory,
) {
    @Throws(NoSuchAlgorithmException::class, KeyManagementException::class)
    suspend fun forAccount(account: CaldavAccount): EtebaseClient = forUrl(
            account.url!!,
            account.username!!,
            null,
            account.getPassword(encryption))

    @Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
    suspend fun forUrl(url: String, username: String, password: String?, session: String? = null, foreground: Boolean = false): EtebaseClient = withContext(Dispatchers.IO) {
        val httpClient = createHttpClient(foreground, username)
        val client = Client.create(httpClient, url)
        val etebase = session
                ?.let { Account.restore(client, it, null) }
                ?: Account.login(client, username, password!!)
        EtebaseClient(context, username, etebase, caldavDao)
    }

    private suspend fun createHttpClient(foreground: Boolean, cookieKey: String): OkHttpClient {
        return httpClientFactory.newClient(
            foreground = foreground,
            cookieKey = cookieKey,
        ) { builder ->
            builder
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
        }
    }
}