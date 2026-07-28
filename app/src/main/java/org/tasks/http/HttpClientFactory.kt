package org.tasks.http

import android.content.Context
import at.bitfire.dav4jvm.okhttp.BasicDigestAuthHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.tasks.BuildConfig
import org.tasks.data.entity.CaldavAccount
import org.tasks.extensions.Context.cookiePersistor
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.microsoft.MicrosoftClientProvider
import org.tasks.sync.microsoft.MicrosoftService
import org.tasks.sync.microsoft.MicrosoftTokenProvider
import timber.log.Timber
import javax.inject.Inject

class HttpClientFactory @Inject constructor(
    @ApplicationContext context: Context,
    private val encryption: KeyStoreEncryption,
    private val microsoftTokenProvider: MicrosoftTokenProvider,
) : AndroidOkHttpClientFactory(
    context = context,
    userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}) Android/${android.os.Build.VERSION.RELEASE}",
), MicrosoftClientProvider {

    override suspend fun getService(account: CaldavAccount): MicrosoftService =
        getMicrosoftService(account)

    override suspend fun hasCredentials(account: CaldavAccount): Boolean =
        microsoftTokenProvider.hasCredentials(account)

    suspend fun newClient(foreground: Boolean) = newClient(
        foreground = foreground,
        cookieKey = null,
        block = {},
    )

    suspend fun newClient(
        foreground: Boolean = false,
        username: String? = null,
        encryptedPassword: String? = null
    ): OkHttpClient {
        val decrypted = encryptedPassword?.let { encryption.decrypt(it) }
        return newClient(foreground = foreground, cookieKey = username) { builder ->
            if (!username.isNullOrBlank() && !decrypted.isNullOrBlank()) {
                val auth = BasicDigestAuthHandler(null, username, decrypted.toCharArray())
                builder.addNetworkInterceptor(auth)
                builder.authenticator(auth)
            }
        }
    }

    suspend fun getMicrosoftService(account: CaldavAccount): MicrosoftService = withContext(Dispatchers.IO) {
        val token = microsoftTokenProvider.getToken(account)
        val client = HttpClient(Android) {
            installMicrosoftGraph(
                cookiesStorage = AndroidCookieStorage(context = context, key = account.username),
                logLevel = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.HEADERS,
                log = { Timber.d(it) },
                bearerToken = { token },
            )
        }
        MicrosoftService(
            client = client
        )
    }
}
