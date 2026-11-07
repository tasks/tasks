package org.tasks.sync.microsoft

import android.content.Context
import io.ktor.client.plugins.logging.LogLevel
import org.tasks.data.dao.CaldavDao
import org.tasks.http.AndroidCookieStorage
import org.tasks.http.OkHttpClientFactory

@Suppress("FunctionName")
fun AndroidMicrosoftClientProvider(
    context: Context,
    okHttpClientFactory: OkHttpClientFactory,
    caldavDao: CaldavDao,
    tokens: MicrosoftTokenSource,
    logLevel: LogLevel = LogLevel.HEADERS,
) = OkHttpMicrosoftClientProvider(
    okHttpClientFactory = okHttpClientFactory,
    caldavDao = caldavDao,
    tokens = tokens,
    cookiesStorage = { AndroidCookieStorage(context, it) },
    logLevel = logLevel,
)
