package org.tasks.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.tasks.InMemoryDataStore
import org.tasks.billing.SubscriptionProvider
import org.tasks.data.dao.CaldavDao
import org.tasks.preferences.TasksPreferences
import org.tasks.sync.SyncRunner.SyncPass

private val fakeSubscriptionProvider =
    object : SubscriptionProvider {
        override val subscription = flow<SubscriptionProvider.SubscriptionInfo?> { emit(null) }
        override suspend fun getFormattedPrice(sku: String): String? = null
    }

class SyncRunnerSyncSourceTest {

    private fun runner(
        caldavDao: CaldavDao,
        tasksPreferences: TasksPreferences,
        pass: suspend (SyncPass) -> Unit,
    ): SyncRunner = SyncRunner(
        scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
        caldavDao = caldavDao,
        subscriptionProvider = { fakeSubscriptionProvider },
        tasksPreferences = tasksPreferences,
        pass = pass,
    )

    @Test
    fun setsSyncSourceDuringPassAndClearsItAfterwards() = runBlocking {
        val caldavDao: CaldavDao = mock()
        whenever(caldavDao.getAccounts()).thenReturn(emptyList())
        val tasksPreferences = TasksPreferences(InMemoryDataStore())
        var sawDuringPass: String? = null
        var passRan = false

        runner(caldavDao, tasksPreferences) { _ ->
            passRan = true
            sawDuringPass = tasksPreferences.get(TasksPreferences.syncSource, "")
        }.syncNow(SyncSource.USER_INITIATED)

        assertTrue(passRan)
        assertEquals(SyncSource.USER_INITIATED.name, sawDuringPass)
        assertEquals(SyncSource.NONE.name, tasksPreferences.get(TasksPreferences.syncSource, ""))
    }

    @Test
    fun clearsSyncSourceWhenThePassThrows() = runBlocking {
        val caldavDao: CaldavDao = mock()
        whenever(caldavDao.getAccounts()).thenReturn(emptyList())
        val tasksPreferences = TasksPreferences(InMemoryDataStore())

        val failed = try {
            runner(caldavDao, tasksPreferences) { _ -> throw IllegalStateException("sync failed") }
                .syncNow(SyncSource.USER_INITIATED)
            false
        } catch (_: IllegalStateException) {
            true
        }
        assertTrue(failed)
        assertEquals(SyncSource.NONE.name, tasksPreferences.get(TasksPreferences.syncSource, ""))
    }
}
