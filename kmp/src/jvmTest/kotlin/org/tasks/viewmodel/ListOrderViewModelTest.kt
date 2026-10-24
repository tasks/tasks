package org.tasks.viewmodel

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.DatabaseTest
import org.tasks.data.NO_ORDER
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar

@OptIn(ExperimentalCoroutinesApi::class)
class ListOrderViewModelTest : DatabaseTest() {

    private val caldavDao = db.caldavDao()
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
        viewModel = ListOrderViewModel(caldavDao = caldavDao, refreshBroadcaster = mock())
    }

    private suspend fun seedAccount(name: String): CaldavAccount {
        val account = CaldavAccount(uuid = UUID.randomUUID().toString(), name = name)
        caldavDao.insert(account)
        return account
    }

    private suspend fun seedCalendar(account: CaldavAccount, name: String): Long {
        val calendar = CaldavCalendar(
            uuid = UUID.randomUUID().toString(),
            name = name,
            account = account.uuid,
        )
        caldavDao.insert(calendar)
        return calendar.id
    }

    private fun loadedState(): ListOrderState = runBlocking {
        withTimeout(5_000) { viewModel.state.first { it.loaded && it.sections.isNotEmpty() } }
    }

    private suspend fun ordersOf(uuid: String): List<Int> =
        caldavDao.getCaldavFilters(uuid).map { it.caldavCalendar.order }

    private suspend fun waitForOrders(uuid: String, expected: Set<Int>) {
        withTimeout(5_000) {
            while (ordersOf(uuid).toSet() != expected) {
                delay(50)
            }
        }
    }

    @Test
    fun `sections load in alphanum order when no manual order`() {
        runBlocking {
            val account = seedAccount("Alex")
            seedCalendar(account, "Bravo")
            seedCalendar(account, "Alpha")
        }
        createViewModel()
        val state = loadedState()
        assertEquals(1, state.sections.size)
        assertEquals(
            listOf("Alpha", "Bravo"),
            state.sections[0].lists.map { it.name },
        )
        assertFalse(state.hasManualOrder)
    }

    @Test
    fun `manually ordered lists sort ahead of unordered ones`() {
        runBlocking {
            val account = seedAccount("Alex")
            seedCalendar(account, "Zulu")
            val alpha = seedCalendar(account, "Alpha")
            caldavDao.setOrder(alpha, 0)
        }
        createViewModel()
        val state = loadedState()
        assertEquals(
            listOf("Alpha", "Zulu"),
            state.sections[0].lists.map { it.name },
        )
        assertTrue(state.hasManualOrder)
    }

    @Test
    fun `onMove reorders within the section`() {
        val alpha: Long
        val bravo: Long
        runBlocking {
            val account = seedAccount("Alex")
            alpha = seedCalendar(account, "Alpha")
            bravo = seedCalendar(account, "Bravo")
        }
        createViewModel()
        val ids = loadedState().sections[0].lists.map { it.calendarId }
        assertEquals(listOf(alpha, bravo), ids)

        viewModel.onMove(ids[0], ids[1])

        val after = viewModel.state.value.sections[0].lists.map { it.calendarId }
        assertEquals(listOf(bravo, alpha), after)
    }

    @Test
    fun `onMove across sections is a no-op`() {
        runBlocking {
            val one = seedAccount("One")
            val two = seedAccount("Two")
            seedCalendar(one, "List one")
            seedCalendar(two, "List two")
        }
        createViewModel()
        val state = loadedState()
        val fromId = state.sections[0].lists.first().calendarId
        val toId = state.sections[1].lists.first().calendarId

        viewModel.onMove(fromId, toId)

        // Unchanged: the first list of the first section is still first.
        assertEquals(fromId, viewModel.state.value.sections[0].lists.first().calendarId)
    }

    @Test
    fun `onDragStopped commits the settled order as zero to n`() {
        val account: CaldavAccount
        runBlocking {
            account = seedAccount("Alex")
            seedCalendar(account, "Alpha")
            seedCalendar(account, "Bravo")
        }
        createViewModel()
        val ids = loadedState().sections[0].lists.map { it.calendarId }

        viewModel.onMove(ids[0], ids[1])
        viewModel.onDragStopped()

        runBlocking {
            waitForOrders(account.uuid!!, setOf(0, 1))
        }
        // Displayed order was [Alpha, Bravo]; Alpha moved to the end, so
        // [Bravo, Alpha] -> cdl_order 0,1.
        val byOrder = runBlocking {
            caldavDao.getCaldavFilters(account.uuid!!).associate {
                it.caldavCalendar.order to it.caldavCalendar.name
            }
        }
        assertEquals("Bravo", byOrder[0])
        assertEquals("Alpha", byOrder[1])
        assertTrue(viewModel.state.value.hasManualOrder)
    }

    @Test
    fun `resetOrder restores alphabetical order`() {
        val account: CaldavAccount
        runBlocking {
            account = seedAccount("Alex")
            val alpha = seedCalendar(account, "Alpha")
            val bravo = seedCalendar(account, "Bravo")
            caldavDao.setOrder(bravo, 0)
            caldavDao.setOrder(alpha, 1)
        }
        createViewModel()
        assertTrue(loadedState().hasManualOrder)

        viewModel.resetOrder()

        runBlocking {
            withTimeout(5_000) {
                while (ordersOf(account.uuid!!).any { it != NO_ORDER }) {
                    delay(50)
                }
            }
            withTimeout(5_000) {
                while (viewModel.state.value.sections.firstOrNull { it.lists.isNotEmpty() }
                        ?.lists?.map { it.name } != listOf("Alpha", "Bravo")
                ) {
                    delay(50)
                }
            }
        }
        val after = viewModel.state.value
        assertFalse(after.hasManualOrder)
        assertEquals(
            listOf("Alpha", "Bravo"),
            after.sections.first { it.lists.isNotEmpty() }.lists.map { it.name },
        )
    }
}
