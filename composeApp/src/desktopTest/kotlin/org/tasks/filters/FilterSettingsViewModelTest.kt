package org.tasks.filters

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.DatabaseTest
import org.tasks.billing.PurchaseState
import org.tasks.data.entity.Filter
import org.tasks.data.entity.Task

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooManyFunctions")
class FilterSettingsViewModelTest : DatabaseTest() {

    private val filterDao = db.filterDao()
    private val taskDao = db.taskDao()
    private val provider = FilterCriteriaProvider(db.tagDataDao(), db.caldavDao())

    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        initialFilter: Filter? = null,
        templateIndex: Int? = null,
    ) = FilterSettingsViewModel(
        filterCriteriaProvider = provider,
        filterDao = filterDao,
        taskDao = taskDao,
        purchaseState = mock<PurchaseState>(),
        isDark = false,
        initialFilter = initialFilter,
        templateIndex = templateIndex,
    )

    private suspend fun waitForCriteria(
        vm: FilterSettingsViewModel,
    ): FilterSettingsViewModel.State {
        // The VM's init (and openAddCriterion) suspend on string-resource resolution, which
        // resumes off the test thread even with Main set to Unconfined. Wait for the state to
        // settle instead of assuming the launch ran inline. Once criteria is non-empty it stays
        // that way (the universe row is always present), so this is a stable loaded signal.
        return withTimeout(5_000) { vm.state.first { it.criteria.isNotEmpty() } }
    }

    private suspend fun openCatalog(vm: FilterSettingsViewModel) {
        vm.openAddCriterion()
        withTimeout(5_000) { vm.state.first { it.newCriterionTypes.isNotEmpty() } }
    }

    @Test
    fun `new filter starts with just the locked universe row`() = runBlocking {
        val vm = viewModel()
        val criteria = waitForCriteria(vm).criteria
        assertEquals(1, criteria.size)
        assertEquals(CriterionInstance.TYPE_UNIVERSE, criteria[0].type)
        assertTrue(vm.isNew)
    }

    @Test
    fun `template starts with template criteria plus the universe row`() = runBlocking {
        // Template 0 is "Custom" (no criteria rows); 1 is "Overdue".
        val vm = viewModel(templateIndex = 1)
        val criteria = waitForCriteria(vm).criteria
        assertEquals(2, criteria.size)
        assertEquals(CriterionInstance.TYPE_UNIVERSE, criteria[0].type)
        assertEquals(CriterionInstance.TYPE_INTERSECT, criteria[1].type)
    }

    @Test
    fun `save requires a non-blank title`() {
        runBlocking {
            val vm = viewModel()
            vm.setTitle("   ")
            assertThrows(IllegalArgumentException::class.java) { runBlocking { vm.save() } }
        }
    }

    @Test
    fun `save inserts the filter with criterion and sql`() = runBlocking {
        val vm = viewModel()
        waitForCriteria(vm)
        vm.setTitle("Work")
        val saved = vm.save()

        assertTrue(saved.id > 0)
        val stored = filterDao.getById(saved.id)!!
        assertEquals("Work", stored.title)
        assertNotNull(stored.criterion)
        assertTrue(stored.criterion!!.isNotBlank())
        assertEquals(vm.state.value.criteria.sql, stored.sql)
    }

    @Test
    fun `edit path reloads criteria from the stored filter and reports no changes`() = runBlocking {
        val vm = viewModel()
        waitForCriteria(vm)
        vm.setTitle("Work")
        val saved = vm.save()

        val editVm = viewModel(initialFilter = saved)
        val criteria = waitForCriteria(editVm).criteria
        assertFalse(editVm.isNew)
        assertEquals("Work", editVm.state.value.title)
        assertTrue(criteria.size >= 1)
        assertFalse(editVm.hasChanges())
    }

    @Test
    fun `adding then removing a boolean criterion returns to the original list`() = runBlocking {
        val vm = viewModel()
        val before = waitForCriteria(vm).criteria.size

        openCatalog(vm)
        val types = vm.state.value.newCriterionTypes
        val booleanIndex = types.indexOfFirst { it is BooleanCriterion }
        assertTrue("catalog should contain a boolean criterion", booleanIndex >= 0)
        vm.onTypePicked(booleanIndex)
        assertEquals(before + 1, vm.state.value.criteria.size)

        vm.removeAt(vm.state.value.criteria.size - 1)
        assertEquals(before, vm.state.value.criteria.size)
    }

    @Test
    fun `text input criterion opens a value picker and stores the entered text`() = runBlocking {
        val vm = viewModel()
        val before = waitForCriteria(vm).criteria.size

        openCatalog(vm)
        val textIndex = vm.state.value.newCriterionTypes.indexOfFirst { it is TextInputCriterion }
        assertTrue("catalog should contain a text-input criterion", textIndex >= 0)
        vm.onTypePicked(textIndex)
        assertNotNull(vm.state.value.newCriterionOptions)
        assertEquals(before, vm.state.value.criteria.size)

        vm.onTextDone("  home  ")
        val last = vm.state.value.criteria.last()
        assertEquals(before + 1, vm.state.value.criteria.size)
        assertEquals("home", last.selectedText)
    }

    @Test
    fun `move reorders criteria while the universe row stays locked`() = runBlocking {
        val vm = viewModel()
        waitForCriteria(vm)

        openCatalog(vm)
        val booleanIndex = vm.state.value.newCriterionTypes.indexOfFirst { it is BooleanCriterion }
        vm.onTypePicked(booleanIndex)

        openCatalog(vm)
        val textIndex = vm.state.value.newCriterionTypes.indexOfFirst { it is TextInputCriterion }
        vm.onTypePicked(textIndex)
        vm.onTextDone("home")

        assertEquals(3, vm.state.value.criteria.size)

        // Swap the two added rows.
        vm.move(1, 2)
        val reordered = vm.state.value.criteria
        assertEquals(3, reordered.size)
        assertEquals(CriterionInstance.TYPE_UNIVERSE, reordered[0].type)
        assertTrue(reordered[1].criterion is TextInputCriterion)
        assertTrue(reordered[2].criterion is BooleanCriterion)

        // The universe row cannot be moved.
        vm.move(0, 1)
        assertEquals(CriterionInstance.TYPE_UNIVERSE, vm.state.value.criteria[0].type)
    }

    @Test
    fun `delete removes the filter from the database`() = runBlocking {
        val vm = viewModel()
        waitForCriteria(vm)
        vm.setTitle("Work")
        val saved = vm.save()
        assertNotNull(filterDao.getById(saved.id))

        viewModel(initialFilter = saved).delete {}
        assertEquals(0, filterDao.getFilters().size)
    }

    @Test
    fun `universe row count reflects seeded tasks`() = runBlocking {
        taskDao.insert(Task(title = "Alpha"))
        val vm = viewModel()
        waitForCriteria(vm)
        val universe = withTimeout(5_000) { vm.state.first { it.criteria[0].end == 1 } }.criteria[0]
        // The first row's start is its own count (ported from the Android view-model).
        assertEquals(1, universe.start)
        assertEquals(1, universe.max)
    }
}
