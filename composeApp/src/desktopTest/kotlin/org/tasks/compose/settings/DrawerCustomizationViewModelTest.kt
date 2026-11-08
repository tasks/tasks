package org.tasks.compose.settings

import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.tasks.DatabaseTest
import org.tasks.InMemoryDataStore
import org.tasks.billing.PurchaseState
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.metadata.TagMetadataSync
import org.tasks.compose.drawer.DrawerConfiguration
import org.tasks.data.NO_ORDER
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Place
import org.tasks.filters.FilterProvider
import org.tasks.preferences.TasksPreferences
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.viewmodel.DrawerCustomizationState
import org.tasks.viewmodel.DrawerCustomizationViewModel

private const val ACCOUNT_NAME = "Alex"
private const val ALPHA = "Alpha"
private const val BRAVO = "Bravo"
private const val ZULU = "Zulu"
private const val TAGS_SECTION = "Tags"
private const val WORK_FILTER = "Work"
private const val OFFICE_PLACE = "Office"

/**
 * Behavioural tests for [DrawerCustomizationViewModel], run against the real
 * [FilterProvider] so the drawer items are exactly what the production code
 * produces. (The kmp jvmTest environment cannot load the real provider:
 * resolving string resources pulls in the skiko native library, which is
 * only available to the desktop test runtime.)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DrawerCustomizationViewModelTest : DatabaseTest() {

    private val tagMetadataSync = mock<TagMetadataSync>()
    private val syncAdapters = mock<SyncAdapters>()

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
        // The view model loads the sections in its init, so the database must
        // be seeded before it is constructed.
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
            tagMetadataSync = tagMetadataSync,
            syncAdapters = syncAdapters,
            refreshBroadcaster = mock<RefreshBroadcaster>(),
        )
    }

    @Test
    fun `sections load in drawer order filters tags places lists`() {
        runBlocking {
            val account = seedAccount(db, ACCOUNT_NAME)
            seedCalendar(db, account, BRAVO)
            seedCalendar(db, account, ALPHA)
            db.tagDataDao().getOrCreateTag(ZULU)
            db.tagDataDao().getOrCreateTag("Alpha tag")
            db.locationDao().insert(Place(name = OFFICE_PLACE))
            db.filterDao().insert(org.tasks.data.entity.Filter(title = WORK_FILTER))
            createViewModel()
        }
        val state = awaitLoadedState(viewModel)
        assertEquals(listOf("Filters", TAGS_SECTION, "Places", ACCOUNT_NAME), state.sections.map { it.title })
        assertEquals(listOf(WORK_FILTER), state.sections[0].items.map { it.name })
        assertEquals(listOf("Alpha tag", ZULU), state.sections[1].items.map { it.name })
        assertEquals(listOf(OFFICE_PLACE), state.sections[2].items.map { it.name })
        assertEquals(listOf(ALPHA, BRAVO), state.sections[3].items.map { it.name })
        assertFalse(state.hasManualOrder)
    }

    @Test
    fun `onMove reorders within a section`() {
        runBlocking {
            db.tagDataDao().getOrCreateTag(ALPHA)
            db.tagDataDao().getOrCreateTag(BRAVO)
            createViewModel()
        }
        val state = awaitLoadedState(viewModel)
        val tagSection = state.sections.single { it.title == TAGS_SECTION }
        val keys = tagSection.items.map { it.key }

        viewModel.onMove(keys[0], keys[1])

        val after = viewModel.state.value.sections.single { it.title == TAGS_SECTION }
        assertEquals(listOf(keys[1], keys[0]), after.items.map { it.key })
    }

    @Test
    fun `onMove across sections is a no-op`() {
        runBlocking {
            val account = seedAccount(db, ACCOUNT_NAME)
            seedCalendar(db, account, "Only list")
            db.tagDataDao().getOrCreateTag("Only tag")
            createViewModel()
        }
        val state = awaitLoadedState(viewModel)
        val fromKey = state.sections.first { it.items.isNotEmpty() }.items.first().key
        val toKey = state.sections.last { it.items.isNotEmpty() }.items.first().key
        val before = state.sections.map { it.items.map { i -> i.key } }

        viewModel.onMove(fromKey, toKey)

        assertEquals(before, viewModel.state.value.sections.map { it.items.map { i -> i.key } })
    }

    @Test
    fun `onDragStopped commits the list section order as zero to n`() {
        val account: CaldavAccount
        runBlocking {
            account = seedAccount(db, ACCOUNT_NAME)
            seedCalendar(db, account, ALPHA)
            seedCalendar(db, account, BRAVO)
            createViewModel()
        }
        val keys = awaitLoadedState(viewModel).sections.single { it.title == ACCOUNT_NAME }.items.map { it.key }

        viewModel.onMove(keys[0], keys[1])
        viewModel.onDragStopped()

        runBlocking {
            withTimeout(10_000) {
                while (
                    db.caldavDao().getCaldavFilters(account.uuid!!)
                        .map { it.caldavCalendar.order }.toSet() != setOf(0, 1)
                ) {
                    delay(50)
                }
            }
        }
        runBlocking { verify(tagMetadataSync, org.mockito.kotlin.never()).markOrderDirty() }
    }

    @Test
    fun `onDragStopped on tags commits tag order and queues the metadata push`() {
        runBlocking {
            db.tagDataDao().getOrCreateTag(ALPHA)
            db.tagDataDao().getOrCreateTag(BRAVO)
            createViewModel()
        }
        val keys = awaitLoadedState(viewModel).sections.single { it.title == TAGS_SECTION }.items.map { it.key }

        viewModel.onMove(keys[0], keys[1])
        viewModel.onDragStopped()

        runBlocking {
            withTimeout(10_000) {
                while (db.tagDataDao().getAll().map { it.order }.toSet() != setOf(0, 1)) {
                    delay(50)
                }
            }
        }
        runBlocking { verify(tagMetadataSync).markOrderDirty() }
        verify(syncAdapters).sync(SyncSource.METADATA_CHANGE)
    }

    @Test
    fun `resetOrder resets all four section types`() {
        val account: CaldavAccount
        runBlocking {
            account = seedAccount(db, ACCOUNT_NAME)
            val alpha = seedCalendarId(db, account, ALPHA)
            val bravo = seedCalendarId(db, account, BRAVO)
            db.caldavDao().setOrder(bravo, 0)
            db.caldavDao().setOrder(alpha, 1)
            val tagZulu = db.tagDataDao().getOrCreateTag(ZULU)
            val tagAlpha = db.tagDataDao().getOrCreateTag(ALPHA)
            db.tagDataDao().setOrder(tagZulu.id!!, 0)
            db.tagDataDao().setOrder(tagAlpha.id!!, 1)
            db.locationDao().insert(Place(name = OFFICE_PLACE))
            db.filterDao().insert(org.tasks.data.entity.Filter(title = WORK_FILTER))
            createViewModel()
        }

        viewModel.resetOrder()

        runBlocking {
            withTimeout(10_000) {
                while (!allOrdersReset(db, account)) {
                    delay(50)
                }
            }
        }
        verify(syncAdapters).sync(SyncSource.METADATA_CHANGE)
    }
}

// Seeding and polling helpers live at top level (instead of as class
// members) so the test class itself stays under the function-count limit.

private fun awaitLoadedState(viewModel: DrawerCustomizationViewModel): DrawerCustomizationState = runBlocking {
    withTimeout(10_000) {
        viewModel.state.first { it.loaded && it.sections.isNotEmpty() }
    }
}

private suspend fun seedAccount(db: Database, name: String): CaldavAccount {
    val account = CaldavAccount(uuid = UUID.randomUUID().toString(), name = name)
    db.caldavDao().insert(account)
    return account
}

private suspend fun seedCalendar(db: Database, account: CaldavAccount, name: String) {
    db.caldavDao().insert(
        CaldavCalendar(
            uuid = UUID.randomUUID().toString(),
            name = name,
            account = account.uuid,
        ),
    )
}

private suspend fun seedCalendarId(db: Database, account: CaldavAccount, name: String): Long {
    val calendar = CaldavCalendar(
        uuid = UUID.randomUUID().toString(),
        name = name,
        account = account.uuid,
    )
    db.caldavDao().insert(calendar)
    return calendar.id
}

// Every section's orders back to NO_ORDER (-1) - the reset assertion.
private suspend fun allOrdersReset(db: Database, account: CaldavAccount): Boolean {
    val orderSets = listOf(
        db.caldavDao().getCaldavFilters(account.uuid!!).map { it.caldavCalendar.order }.toSet(),
        db.tagDataDao().getAll().map { it.order }.toSet(),
        db.locationDao().getPlaces().map { it.order }.toSet(),
        db.filterDao().getFilters().map { it.order }.toSet(),
    )
    return orderSets.all { it == setOf(NO_ORDER) }
}
