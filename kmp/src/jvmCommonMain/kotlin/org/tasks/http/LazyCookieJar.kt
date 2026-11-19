package org.tasks.http

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class LazyCookieJar(create: () -> CookieJar) : CookieJar {
    private val delegate: CookieJar by lazy(create)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) =
        delegate.saveFromResponse(url, cookies)

    override fun loadForRequest(url: HttpUrl): List<Cookie> = delegate.loadForRequest(url)
}
