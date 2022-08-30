package org.tasks.http

import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response
import org.tasks.BuildConfig
import java.io.IOException
import java.util.*

object UserAgentInterceptor : Interceptor {
    private val userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} (okhttp3) Android/${Build.VERSION.RELEASE}"

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val locale = Locale.getDefault()
        val request = chain.request().newBuilder()
            .header("User-Agent", userAgent)
            .header("Accept-Language", locale.language + "-" + locale.country + ", " + locale.language + ";q=0.7, *;q=0.5")
            .build()
        return chain.proceed(request)
    }
}
