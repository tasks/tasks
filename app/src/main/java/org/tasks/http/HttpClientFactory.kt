package org.tasks.http

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import org.tasks.BuildConfig
import org.tasks.security.KeyStoreEncryption
import javax.inject.Inject

class HttpClientFactory @Inject constructor(
    @ApplicationContext context: Context,
    private val encryption: KeyStoreEncryption,
) : AndroidOkHttpClientFactory(
    context = context,
    userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}) Android/${android.os.Build.VERSION.RELEASE}",
) {

    suspend fun newClient(foreground: Boolean) = newClient(
        foreground = foreground,
        cookieKey = null,
        block = {},
    )

    suspend fun newAuthenticatedClient(
        foreground: Boolean = false,
        username: String? = null,
        encryptedPassword: String? = null,
        url: String? = null,
    ): HttpClient {
        val decrypted = encryptedPassword?.let { encryption.decrypt(it) }
        return newClient(foreground = foreground, cookieKey = username).toKtor {
            if (!username.isNullOrBlank() && !decrypted.isNullOrBlank()) {
                installBasicDigestAuth(username, decrypted, url)
            }
        }
    }
}
