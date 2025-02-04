package org.tasks.http

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.dav4jvm.BasicDigestAuthHandler
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
import net.openid.appauth.AuthState
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import org.tasks.BuildConfig
import org.tasks.caldav.TasksCookieJar
import org.tasks.data.entity.CaldavAccount
import org.tasks.extensions.Context.cookiePersistor
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.microsoft.MicrosoftService
import org.tasks.sync.microsoft.requestTokenRefresh
import timber.log.Timber
import javax.inject.Inject
import javax.net.ssl.SSLContext

class HttpClientFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryption: KeyStoreEncryption,
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
                val auth = BasicDigestAuthHandler(null, username, decrypted)
                builder.addNetworkInterceptor(auth)
                builder.authenticator(auth)
            }
        }
    }

    suspend fun newClient(
        foreground: Boolean = false,
        cookieKey: String? = null,
        block: (OkHttpClient.Builder) -> Unit = {}
    ): OkHttpClient {
        val customCertManager = withContext(Dispatchers.Default) {
            CustomCertManager(context)
        }
        customCertManager.appInForeground = foreground
        val hostnameVerifier = customCertManager.hostnameVerifier(OkHostnameVerifier)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(customCertManager), null)
        val builder = OkHttpClient()
            .newBuilder()
            .followRedirects(false)
            .followSslRedirects(true)
            .sslSocketFactory(sslContext.socketFactory, customCertManager)
            .hostnameVerifier(hostnameVerifier)
            .addInterceptor(UserAgentInterceptor)
            .cookieJar(TasksCookieJar(persistor = context.cookiePersistor(cookieKey)))

        block(builder)

        return builder.build()
    }

    suspend fun getMicrosoftService(account: CaldavAccount): MicrosoftService {
        val authState = encryption.decrypt(account.password)?.let { AuthState.jsonDeserialize(it) }
            ?: throw RuntimeException("Missing credentials")
        if (authState.needsTokenRefresh) {
            val (token, ex) = context.requestTokenRefresh(authState)
            authState.update(token, ex)
            if (authState.isAuthorized) {
                account.password = encryption.encrypt(authState.jsonSerializeString())
            }
        }
        if (!authState.isAuthorized) {
            throw RuntimeException("Needs authentication")
        }
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
                header("Authorization", "Bearer ${authState.accessToken}")
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
        return MicrosoftService(
            client = client
        )
    }
}
