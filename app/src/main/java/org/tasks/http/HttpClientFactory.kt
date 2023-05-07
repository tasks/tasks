package org.tasks.http

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.dav4jvm.BasicDigestAuthHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import org.tasks.DebugNetworkInterceptor
import org.tasks.caldav.TasksCookieJar
import org.tasks.data.CaldavAccount
import org.tasks.extensions.Context.cookiePersistor
import org.tasks.preferences.Preferences
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.microsoft.MicrosoftService
import org.tasks.sync.microsoft.requestTokenRefresh
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.net.ssl.SSLContext

class HttpClientFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val interceptor: DebugNetworkInterceptor,
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

        if (preferences.isFlipperEnabled) {
            interceptor.apply(builder)
        }
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
        val client = newClient(cookieKey = account.username) {
            it.addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer ${authState.accessToken}")
                        .build()
                )
            }
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(URL_MICROSOFT)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(MicrosoftService::class.java)
    }

    companion object {
        const val URL_MICROSOFT = "https://graph.microsoft.com"
        val MEDIA_TYPE_JSON = "application/json".toMediaType()
    }
}
