/*
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tasks.auth

import android.content.Context
import android.net.Uri
import at.bitfire.cert4android.CustomCertManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.openid.appauth.Preconditions
import net.openid.appauth.connectivity.ConnectionBuilder
import okhttp3.internal.tls.OkHostnameVerifier
import org.tasks.DebugNetworkInterceptor
import org.tasks.preferences.Preferences
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

/**
 * Creates [HttpURLConnection] instances using the default, platform-provided
 * mechanism, with sensible production defaults.
 */
class DebugConnectionBuilder @Inject constructor(
        @ApplicationContext private val context: Context,
        private val interceptor: DebugNetworkInterceptor,
        private val preferences: Preferences,
) : ConnectionBuilder {

    var appInForeground: Boolean = true

    @Throws(IOException::class)
    override fun openConnection(uri: Uri): HttpURLConnection {
        Preconditions.checkNotNull(uri, "url must not be null")
        Preconditions.checkArgument(HTTPS_SCHEME == uri.scheme,
                "only https connections are permitted")
        val customCertManager = CustomCertManager(context)
        customCertManager.appInForeground = appInForeground
        val hostnameVerifier = customCertManager.hostnameVerifier(OkHostnameVerifier)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(customCertManager), null)
        val conn = URL(uri.toString()).openConnection() as HttpsURLConnection
        conn.connectTimeout = CONNECTION_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.instanceFollowRedirects = false
        conn.hostnameVerifier = hostnameVerifier
        conn.sslSocketFactory = sslContext.socketFactory
        return conn
    }

    companion object {
        private val CONNECTION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15).toInt()
        private val READ_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10).toInt()
        private const val HTTPS_SCHEME = "https"
    }
}