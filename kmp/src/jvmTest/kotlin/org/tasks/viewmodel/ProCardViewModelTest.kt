package org.tasks.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.tasks.InMemoryDataStore
import org.tasks.PlatformConfiguration
import org.tasks.auth.TasksServerEnvironment
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.compose.settings.ProCardState
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences

@OptIn(ExperimentalCoroutinesApi::class)
class ProCardViewModelTest {

    private val accounts = MutableStateFlow<List<CaldavAccount>?>(null)
    private val preferences = TasksPreferences(InMemoryDataStore())

    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `state stays unloaded until the accounts are read`() = runBlocking {
        val viewModel = viewModel()
        val collector = launch(Dispatchers.Unconfined) { viewModel.state.collect { } }

        assertNull(viewModel.state.value)

        accounts.value = listOf(TASKS_ACCOUNT, CALDAV_ACCOUNT)

        val state = viewModel.state.value!!
        assertTrue(state.proCard is ProCardState.TasksOrgAccount)
        assertEquals(listOf(CALDAV_ACCOUNT), state.accounts)
        collector.cancel()
    }

    private fun viewModel() = ProCardViewModel(
        caldavDao = mock<CaldavDao> { on { watchAccounts() } doReturn accounts.filterNotNull() },
        subscriptionProvider = mock<SubscriptionProvider> {
            on { subscription } doReturn flowOf(null)
        },
        tasksPreferences = preferences,
        accountDataRepository = mock<TasksAccountDataRepository>(),
        serverEnvironment = TasksServerEnvironment(preferences),
        platformConfiguration = PlatformConfiguration(),
    )

    companion object {
        private val TASKS_ACCOUNT = CaldavAccount(
            id = 1,
            uuid = "tasks",
            accountType = CaldavAccount.TYPE_TASKS,
        )
        private val CALDAV_ACCOUNT = CaldavAccount(id = 2, uuid = "caldav", name = "CalDAV")
    }
}
