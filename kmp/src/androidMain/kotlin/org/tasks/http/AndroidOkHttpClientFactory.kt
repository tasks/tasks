package org.tasks.http

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.cert4android.CustomCertStore
import at.bitfire.cert4android.SettingsProvider
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import org.tasks.caldav.TasksCookieJar
import javax.net.ssl.SSLContext

open class AndroidOkHttpClientFactory(
    protected val context: Context,
    private val userAgent: String,
) : OkHttpClientFactory {

    override suspend fun newClient(
        foreground: Boolean,
        cookieKey: String?,
        block: (OkHttpClient.Builder) -> Unit,
    ): OkHttpClient {
        val customCertManager = withContext(Dispatchers.Default) {
            CustomCertManager(
                certStore = CustomCertStore.getInstance(context),
                settings = object : SettingsProvider {
                    override val appInForeground = foreground
                    override val trustSystemCerts = true
                }
            )
        }
        val hostnameVerifier = customCertManager.HostnameVerifier(OkHostnameVerifier)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(customCertManager), null)
        val builder = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(true)
            .sslSocketFactory(sslContext.socketFactory, customCertManager)
            .hostnameVerifier(hostnameVerifier)
            .addInterceptor(UserAgentInterceptor(userAgent))
            .cookieJar(TasksCookieJar(persistor = cookiePersistor(cookieKey)))

        block(builder)

        return builder.build()
    }

    private fun cookiePersistor(key: String?) = SharedPrefsCookiePersistor(
        context.getSharedPreferences(
            "CookiePersistence${key?.let { "_$it" } ?: ""}",
            Context.MODE_PRIVATE,
        )
    )
}
