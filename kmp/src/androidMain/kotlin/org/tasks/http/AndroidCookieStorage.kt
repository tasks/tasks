package org.tasks.http

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("FunctionName")
fun AndroidCookieStorage(context: Context, key: String?) = PersistentCookiesStorage(
    SharedPreferencesCookieBackend(
        context.getSharedPreferences("cookies_$key", Context.MODE_PRIVATE)
    )
)

internal class SharedPreferencesCookieBackend(
    private val prefs: SharedPreferences,
) : CookieBackend {
    override suspend fun read(): List<String> = withContext(Dispatchers.IO) {
        val stale = prefs.all.keys - COOKIES
        if (stale.isNotEmpty()) {
            prefs.edit().apply { stale.forEach { remove(it) } }.apply()
        }
        prefs.getStringSet(COOKIES, null)?.toList() ?: emptyList()
    }

    override suspend fun write(cookies: List<String>): Boolean = withContext(Dispatchers.IO) {
        prefs.edit().putStringSet(COOKIES, cookies.toSet()).commit()
    }

    override suspend fun delete() {
        withContext(Dispatchers.IO) { prefs.edit().clear().commit() }
    }

    private companion object {
        const val COOKIES = "/cookies"
    }
}
