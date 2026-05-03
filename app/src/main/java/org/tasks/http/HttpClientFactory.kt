package org.tasks.http

import android.content.Context
import at.bitfire.dav4jvm.okhttp.BasicDigestAuthHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.tasks.BuildConfig
import org.tasks.data.entity.CaldavAccount
import org.tasks.extensions.Context.cookiePersistor
import org.tasks.security.KeyStoreEncryption
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
) {
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
            expectSuccess = true

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }

            defaultRequest {
                header("Authorization", "Bearer $token")
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
            }

            install(HttpCookies) {
                storage = AndroidCookieStorage(context = context, key = account.username)
            }

            install(HttpErrorHandler)

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Timber.d(message)
                    }
                }
                level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.HEADERS
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }
        }
        MicrosoftService(
            client = client
        )
    }
}
