package org.tasks.http

import android.content.Context
import androidx.core.content.edit
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.matches
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.parseServerSetCookieHeader
import timber.log.Timber

class AndroidCookieStorage(
    context: Context,
    key: String?,
) : CookiesStorage {
    private val prefs = context.getSharedPreferences("cookies_$key", Context.MODE_PRIVATE)
    private val cookies = mutableMapOf<String, Cookie>()

    override suspend fun get(requestUrl: Url): List<Cookie> =
        cookies.values.filter { it.matches(requestUrl) }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        cookies[cookie.name] = cookie
        prefs.edit {
            putString(cookie.name, cookie.toString())
        }
    }

    override fun close() {
    }

    init {
        prefs.all.forEach { (name, value) ->
            if (value is String) {
                cookies[name] = parseServerSetCookieHeader(value)
            } else {
                Timber.e("Invalid cookie: $name -> $value")
            }
        }
    }
}
