package org.tasks.auth

import okhttp3.CookieJar
import org.tasks.http.OkHttpClientFactory
import org.tasks.http.toKtor

fun OkHttpClientFactory.oauthClient(foreground: Boolean): TasksOAuthClient = TasksOAuthClient {
    newClient(foreground) { it.cookieJar(CookieJar.NO_COOKIES) }
        .toKtor { oauthDefaults() }
}
