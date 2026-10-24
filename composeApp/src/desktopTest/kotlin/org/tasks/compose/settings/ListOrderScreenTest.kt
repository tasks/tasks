package org.tasks.compose.settings

import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.DatabaseTest
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.tasks.viewmodel.ListOrderViewModel

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
class ListOrderScreenTest : DatabaseTest() {

    private lateinit var viewModel: ListOrderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * The view model loads the sections in [ListOrderViewModel.init], so the
     * database must be seeded before it is constructed.
     */
    private fun createViewModel() {
        viewModel = ListOrderViewModel(
            caldavDao = db.caldavDao(),
            refreshBroadcaster = mock<RefreshBroadcaster>(),
        )
    }

    private fun awaitLoaded() {
        runBlocking {
            withTimeout(5_000) { viewModel.state.first { it.loaded && it.sections.isNotEmpty() } }
        }
    }

    @Test
    fun `renders account sections and their lists`() {
        val accountName = "Work"
        runBlocking {
            val account = CaldavAccount(uuid = UUID.randomUUID().toString(), name = accountName)
            db.caldavDao().insert(account)
            listOf("Alpha", "Bravo").forEach {
                db.caldavDao().insert(
                    CaldavCalendar(uuid = UUID.randomUUID().toString(), name = it, account = account.uuid)
                )
            }
        }
        createViewModel()
        awaitLoaded()

        runComposeUiTest {
            setContent { ListOrderScreen(viewModel = viewModel) }
            onNodeWithText(accountName).assertIsDisplayed()
            onNodeWithText("Alpha").assertIsDisplayed()
            onNodeWithText("Bravo").assertIsDisplayed()
        }
    }

    @Test
    fun `rows reflect a view model move`() {
        runBlocking {
            val account = db.caldavDao().let { dao ->
                val a = CaldavAccount(uuid = UUID.randomUUID().toString(), name = "Alex")
                dao.insert(a)
                a
            }
            listOf("First", "Second", "Third").forEach {
                db.caldavDao().insert(
                    CaldavCalendar(uuid = UUID.randomUUID().toString(), name = it, account = account.uuid)
                )
            }
        }
        createViewModel()
        awaitLoaded()
        val ids = runBlocking { viewModel.state.value }.sections.single().lists.map { it.calendarId }
        viewModel.onMove(ids.first(), ids.last())

        runComposeUiTest {
            setContent { ListOrderScreen(viewModel = viewModel) }
            onNodeWithText("Alex").assertIsDisplayed()
            // The dragged row ("First") is still present after the move.
            onNodeWithText("First").assertIsDisplayed()
        }
    }
}
