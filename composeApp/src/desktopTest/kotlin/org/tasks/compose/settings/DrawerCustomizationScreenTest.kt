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
import org.tasks.InMemoryDataStore
import org.tasks.billing.PurchaseState
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.metadata.TagMetadataSync
import org.tasks.compose.drawer.DrawerConfiguration
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.filters.FilterProvider
import org.tasks.preferences.TasksPreferences
import org.tasks.sync.SyncAdapters
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.tasks.viewmodel.DrawerCustomizationViewModel

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
class DrawerCustomizationScreenTest : DatabaseTest() {

    private lateinit var viewModel: DrawerCustomizationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        // The view model loads the sections in its init, so the database
        // must be seeded before it is constructed.
        val db = db
        viewModel = DrawerCustomizationViewModel(
            filterProvider = FilterProvider(
                filterDao = db.filterDao(),
                tagDataDao = db.tagDataDao(),
                caldavDao = db.caldavDao(),
                configuration = object : DrawerConfiguration {},
                locationDao = db.locationDao(),
                taskDao = db.taskDao(),
                tasksPreferences = TasksPreferences(InMemoryDataStore()),
                purchaseState = object : PurchaseState {
                    override val hasTasksAccount = false
                    override val hasPro = false
                    override val hasTasksSubscription = false
                },
            ),
            caldavDao = db.caldavDao(),
            tagDataDao = db.tagDataDao(),
            locationDao = db.locationDao(),
            filterDao = db.filterDao(),
            tagMetadataSync = mock<TagMetadataSync>(),
            syncAdapters = mock<SyncAdapters>(),
            refreshBroadcaster = mock<RefreshBroadcaster>(),
        )
    }

    private fun awaitLoaded() {
        runBlocking {
            withTimeout(5_000) {
                viewModel.state.first { it.loaded && it.sections.isNotEmpty() }
            }
        }
    }

    @Test
    fun `renders the drawer sections and their rows`() {
        val accountName = "Work"
        runBlocking {
            val account = CaldavAccount(uuid = UUID.randomUUID().toString(), name = accountName)
            db.caldavDao().insert(account)
            listOf("Alpha", "Bravo").forEach {
                db.caldavDao().insert(
                    CaldavCalendar(uuid = UUID.randomUUID().toString(), name = it, account = account.uuid)
                )
            }
            db.tagDataDao().getOrCreateTag("Urgent")
            db.locationDao().insert(org.tasks.data.entity.Place(name = "Home"))
        }
        createViewModel()
        awaitLoaded()

        runComposeUiTest {
            setContent { DrawerCustomizationScreen(viewModel = viewModel) }
            onNodeWithText(accountName).assertIsDisplayed()
            onNodeWithText("Alpha").assertIsDisplayed()
            onNodeWithText("Bravo").assertIsDisplayed()
            onNodeWithText("Urgent").assertIsDisplayed()
            onNodeWithText("Home").assertIsDisplayed()
        }
    }

    @Test
    fun `rows remain present after a view model move`() {
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
        val ids = runBlocking { viewModel.state.value }
            .sections.single { it.title == "Alex" }
            .items.map { it.id }
        viewModel.onMove("list-${ids.first()}", "list-${ids.last()}")

        runComposeUiTest {
            setContent { DrawerCustomizationScreen(viewModel = viewModel) }
            onNodeWithText("Alex").assertIsDisplayed()
            // The dragged row is still present after the move.
            onNodeWithText("First").assertIsDisplayed()
            onNodeWithText("Third").assertIsDisplayed()
        }
    }
}
