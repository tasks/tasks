package org.tasks.caldav

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences
import org.tasks.preferences.TasksPreferences.Companion.cachedAccountData
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasksAccountDataRepository @Inject constructor(
    private val provider: CaldavClientProvider,
    private val caldavDao: CaldavDao,
    private val tasksPreferences: TasksPreferences,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    val accountResponseFlow: Flow<TasksAccountResponse?> =
        tasksPreferences.flow(cachedAccountData, "")
            .map { raw ->
                raw.takeIf(String::isNotBlank)?.let { parseResponse(it) }
            }

    suspend fun getAccountResponse(): TasksAccountResponse? {
        val raw = tasksPreferences.get(cachedAccountData, "")
        return raw.takeIf(String::isNotBlank)?.let { parseResponse(it) }
    }

    suspend fun fetchAndCache(account: CaldavAccount): TasksAccountResponse? = mutex.withLock {
        val raw = provider.forTasksAccount(account).getAccount() ?: return@withLock null
        tasksPreferences.set(cachedAccountData, raw)
        parseResponse(raw)
    }

    suspend fun fetchAndCache(): TasksAccountResponse? {
        val account = caldavDao.getAccounts().firstOrNull { it.isTasksOrg } ?: return null
        return fetchAndCache(account)
    }

    suspend fun clear() {
        tasksPreferences.set(cachedAccountData, "")
    }

    private fun parseResponse(raw: String): TasksAccountResponse? =
        try {
            json.decodeFromString<TasksAccountResponse>(raw)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse account data")
            null
        }
}
