package org.tasks.caldav

import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.CookieCache
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor
import okhttp3.Cookie
import okhttp3.HttpUrl

class TasksCookieJar(
    private val cache: CookieCache = SetCookieCache(),
    private val persistor: CookiePersistor
) : PersistentCookieJar(cache, persistor) {
    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cache.addAll(cookies)
        persistor.saveAll(cookies)
    }
}