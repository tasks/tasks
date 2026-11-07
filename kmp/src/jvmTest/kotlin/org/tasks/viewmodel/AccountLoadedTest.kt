package org.tasks.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.tasks.InMemoryDataStore
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences

@OptIn(ExperimentalCoroutinesApi::class)
class AccountLoadedTest {

    private val rows = MutableSharedFlow<CaldavAccount?>(replay = 1)

    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the caldav editor waits for the watch and then seeds from it`() = runBlocking {
        val viewModel = caldavViewModel()
        viewModel.setAccountId(ACCOUNT_ID)

        assertFalse(viewModel.state.value.loaded)

        rows.emit(account())
        withTimeout(TIMEOUT) { viewModel.state.first { it.loaded } }

        assertEquals("user@example.com", viewModel.state.value.username)
    }

    @Test
    fun `a caldav row that is gone still counts as loaded`() = runBlocking {
        val viewModel = caldavViewModel()
        viewModel.setAccountId(ACCOUNT_ID)

        rows.emit(null)
        withTimeout(TIMEOUT) { viewModel.state.first { it.loaded } }

        assertEquals(null, viewModel.state.value.account)
    }

    @Test
    fun `the etebase editor waits for the watch`() = runBlocking {
        val viewModel = EtebaseAccountSettingsViewModel(
            caldavDao = dao(),
            clientProvider = mock(),
            encryption = mock(),
            taskDeleter = mock(),
            reporting = mock(),
        )
        viewModel.setAccountId(ACCOUNT_ID)

        assertFalse(viewModel.state.value.loaded)

        rows.emit(null)
        withTimeout(TIMEOUT) { viewModel.state.first { it.loaded } }

        assertEquals(null, viewModel.state.value.account)
    }

    @Test
    fun `the google tasks editor waits for the watch`() = runBlocking {
        val viewModel = GoogleTasksAccountViewModel(caldavDao = dao(), taskDeleter = mock())
        viewModel.setAccountId(ACCOUNT_ID)

        assertFalse(viewModel.state.value.loaded)

        rows.emit(account())
        withTimeout(TIMEOUT) { viewModel.state.first { it.loaded } }

        assertEquals("CalDAV", viewModel.state.value.account?.name)
    }

    @Test
    fun `the local list editor waits for the watch`() = runBlocking {
        val viewModel = LocalAccountViewModel(
            caldavDao = dao(),
            taskDeleter = mock(),
            backgroundWork = mock(),
            purchaseState = mock(),
        )
        viewModel.setAccountId(ACCOUNT_ID)

        assertFalse(viewModel.loaded.value)

        rows.emit(account())
        withTimeout(TIMEOUT) { viewModel.loaded.first { it } }

        assertEquals("CalDAV", viewModel.displayName.value)
    }

    @Test
    fun `a local row that is gone still counts as loaded`() = runBlocking {
        val viewModel = LocalAccountViewModel(
            caldavDao = dao(),
            taskDeleter = mock(),
            backgroundWork = mock(),
            purchaseState = mock(),
        )
        viewModel.setAccountId(ACCOUNT_ID)

        rows.emit(null)
        withTimeout(TIMEOUT) { viewModel.loaded.first { it } }

        assertEquals(null, viewModel.account.value)
    }

    @Test
    fun `the opentasks editor waits for the watch`() = runBlocking {
        val viewModel = OpenTaskAccountViewModel(caldavDao = dao())
        viewModel.setAccountId(ACCOUNT_ID)

        assertFalse(viewModel.loaded.value)

        rows.emit(account())
        withTimeout(TIMEOUT) { viewModel.loaded.first { it } }

        assertEquals("CalDAV", viewModel.displayName.value)
    }

    @Test
    fun `the tasks org account screen waits for the watch`() = runBlocking {
        val accounts = MutableSharedFlow<List<CaldavAccount>>(replay = 1)
        val viewModel = TasksAccountViewModel(
            provider = mock(),
            reporting = mock(),
            accountDataRepository = mock<TasksAccountDataRepository> {
                on { accountResponseFlow } doReturn flowOf(null)
            },
            caldavDao = mock<CaldavDao> {
                on { watchAccounts() } doReturn accounts
                on { subscribeToCalendars() } doReturn flowOf(emptyList())
            },
            principalDao = mock(),
            backgroundWork = mock(),
            pushTokenManager = mock(),
            taskDeleter = mock(),
            tasksPreferences = TasksPreferences(InMemoryDataStore()),
            subscriptionProvider = mock<SubscriptionProvider> {
                on { subscription } doReturn flowOf(null)
            },
            caldavUrl = "https://caldav.tasks.org",
        )
        viewModel.setAccountUuid(UUID)

        assertFalse(viewModel.state.value.loaded)

        accounts.emit(listOf(account(uuid = UUID)))
        withTimeout(TIMEOUT) { viewModel.state.first { it.loaded } }

        assertTrue(viewModel.state.value.account != null)
    }

    private fun caldavViewModel() = CaldavAccountSettingsViewModel(
        caldavDao = dao(),
        caldavClientProvider = mock(),
        encryption = mock(),
        taskDeleter = mock(),
        reporting = mock(),
        tagMetadataSync = mock(),
        tagMetadataActivation = mock(),
    )

    private fun dao() = mock<CaldavDao> {
        on { watchAccount(ACCOUNT_ID) } doReturn rows
        on { watchAccounts() } doReturn flowOf(emptyList())
        on { watchTaskCount(UUID) } doReturn flowOf(0)
    }

    private fun account(uuid: String = UUID) = CaldavAccount(
        id = ACCOUNT_ID,
        uuid = uuid,
        name = "CalDAV",
        url = "https://example.com/dav/",
        username = "user@example.com",
    )

    companion object {
        private const val ACCOUNT_ID = 1L
        private const val UUID = "account-uuid"
        private const val TIMEOUT = 5_000L
    }
}
