package org.tasks.sync.microsoft

import io.ktor.client.plugins.logging.LogLevel
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.http.OkHttpClientFactory
import org.tasks.http.PersistentCookiesStorage
import org.tasks.http.newEngineWithoutCookies

@Suppress("FunctionName")
fun OkHttpMicrosoftClientProvider(
    okHttpClientFactory: OkHttpClientFactory,
    caldavDao: CaldavDao,
    tokens: MicrosoftTokenSource,
    cookiesStorage: (key: String) -> PersistentCookiesStorage,
    logLevel: LogLevel = LogLevel.HEADERS,
) = DefaultMicrosoftClientProvider(
    engine = { okHttpClientFactory.newEngineWithoutCookies() },
    cookiesStorage = cookiesStorage,
    tokens = tokens,
    accountIds = { caldavDao.getAccounts(TYPE_MICROSOFT).mapTo(HashSet()) { it.id } },
    logLevel = logLevel,
)
