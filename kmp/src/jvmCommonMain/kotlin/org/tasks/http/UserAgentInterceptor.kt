package org.tasks.http

import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale

internal class UserAgentInterceptor(private val userAgent: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val locale = Locale.getDefault()
        val request = chain.request().newBuilder()
            .header("User-Agent", userAgent)
            .header(
                "Accept-Language",
                locale.language + "-" + locale.country + ", " + locale.language + ";q=0.7, *;q=0.5"
            )
            .build()
        return chain.proceed(request)
    }
}
