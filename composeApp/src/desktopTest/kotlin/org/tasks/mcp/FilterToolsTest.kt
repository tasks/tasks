package org.tasks.mcp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import org.tasks.api.FilterQuery
import org.tasks.data.UUIDHelper
import org.tasks.data.entity.Filter
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CustomFilter
import org.tasks.filters.FilterPreferenceCodec
import org.tasks.filters.TagFilter
import org.tasks.preferences.TasksPreferences

class FilterToolsTest : McpGraphTestCase() {

    @Test
    fun listReturnsSavedFiltersWithoutSqlByDefault() = runBlocking {
        val plain = seedFilter(title = "Due today", sql = " WHERE tasks.dueDate > NOW()", order = 1)
        seedFilter(title = "Work", sql = " WHERE tasks._id IN (SELECT task_id FROM for_tags)", order = 2)

        val page = filtersApi.list()
        val rows = page.rows

        assertEquals(2, page.total)
        assertEquals(2, rows.size)
        assertEquals(plain, rows.first().id)
        assertEquals("Due today", rows.first().title)
        assertEquals(1, rows.first().order)
        rows.forEach { assertEquals("sql must stay hidden by default", null, it.sql) }
    }

    @Test
    fun includeSqlReturnsTheStoredTemplateUnchanged() = runBlocking {
        val sql = " WHERE tasks.dueDate > NOW() - 86400000"
        val id = seedFilter(title = "Due window", sql = sql)

        val row = filtersApi.list(includeSql = true).rows.single()

        assertEquals(id, row.id)
        assertEquals(sql, row.sql)
    }

    @Test
    fun tasksPassingTheFilterComeBackWithTheRegularTaskMapping() = runBlocking {
        val recent = seedTask(title = "Recent", dueDate = System.currentTimeMillis() + DAY)
        seedTask(title = "Far away", dueDate = System.currentTimeMillis() + 10 * DAY)
        seedTask(title = "No date")

        val page = filtersApi.tasks(dueWindowFilterId(), FilterQuery(limit = 10))

        assertEquals(1, page.total)
        assertEquals(listOf(recent), page.rows.map { it.id })
        assertEquals("Recent", page.rows.single().title)
    }

    @Test
    fun permaSqlPlaceholdersResolveAtQueryTime() = runBlocking {
        // A stored filter keeps NOW() in its SQL. If it were executed verbatim, SQLite
        // would reject it as "no such function: NOW" - this test passing proves the
        // substitution happened, and that the match set is time-relative, not stale.
        val id = seedFilter(
            title = "Due window",
            sql = " WHERE tasks.dueDate > NOW() - 86400000 AND tasks.dueDate <= NOW() + 86400000",
        )
        seedTask(title = "Due tomorrow", dueDate = System.currentTimeMillis() + DAY)
        seedTask(title = "Due in a fortnight", dueDate = System.currentTimeMillis() + 14 * DAY)

        val page = filtersApi.tasks(id, FilterQuery())

        assertEquals(1, page.total)
        assertEquals("Due tomorrow", page.rows.single().title)
    }

    @Test
    fun deletedTasksDoNotPass() = runBlocking {
        seedTask(title = "Alive")
        db.taskDao().insert(
            Task(title = "Gone", remoteId = UUIDHelper.newUUID(), deletionDate = 1),
        )

        val page = filtersApi.tasks(titleFilterId("%"), FilterQuery())

        assertEquals(listOf("Alive"), page.rows.map { it.title })
    }

    @Test
    fun sortAndPagingBehaveLikeListTasks() = runBlocking {
        repeat(5) { seedTask(title = "Item $it") }
        val filterId = titleFilterId("Item")

        val first = filtersApi.tasks(filterId, FilterQuery(sort = "title", limit = 2))
        assertEquals(5, first.total)
        assertEquals(listOf("Item 0", "Item 1"), first.rows.map { it.title })
        assertTrue(first.hasMore == true)

        val next = filtersApi.tasks(filterId, FilterQuery(sort = "title", limit = 2, offset = 2))
        assertEquals(listOf("Item 2", "Item 3"), next.rows.map { it.title })

        val last = filtersApi.tasks(filterId, FilterQuery(sort = "title", sortDesc = true, limit = 1))
        assertEquals(listOf("Item 4"), last.rows.map { it.title })
    }

    @Test
    fun aZeroLimitCountsWithoutTransferring() = runBlocking {
        repeat(3) { seedTask(title = "Counted $it") }

        val page = filtersApi.tasks(titleFilterId("Counted"), FilterQuery(limit = 0))

        assertEquals(3, page.total)
        assertTrue(page.rows.isEmpty())
    }

    @Test
    fun anUnknownFilterIdIsACleanError() = runBlocking {
        val error = runCatching { filtersApi.tasks(999, FilterQuery()) }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error!!.message!!.contains("No filter with id 999"))
    }

    @Test
    fun aFilterWithoutStoredSqlCannotBeRun() = runBlocking {
        val id = db.filterDao().insert(Filter(title = "Broken"))

        val error = runCatching { filtersApi.tasks(id, FilterQuery()) }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error!!.message!!.contains("no stored SQL"))
    }

    @Test
    fun theReadToolsJoinTheCatalogInTheirOwnGroup() {
        val filters = McpToolCatalog.tools.filter { it.group == McpToolGroup.Filters }

        assertEquals(listOf("list_filters", "get_filter_tasks", "get_selected_filter"), filters.map { it.name })
        assertTrue(filters.none { it.write })
        assertTrue("list_filters in read tools", "list_filters" in McpToolCatalog.readTools)
        assertTrue("get_filter_tasks in read tools", "get_filter_tasks" in McpToolCatalog.readTools)
        assertTrue("get_selected_filter in read tools", "get_selected_filter" in McpToolCatalog.readTools)
        assertTrue(ToolPermissions.of(AccessMode.ReadOnly).allows("list_filters"))
        assertTrue(ToolPermissions.of(AccessMode.ReadOnly).allows("get_filter_tasks"))
        assertTrue(ToolPermissions.of(AccessMode.ReadOnly).allows("get_selected_filter"))
        assertTrue(ToolPermissions.of(AccessMode.ReadOnly).allows("list_tasks"))
    }

    @Test
    fun noRecordedSelectionMeansNoSelection() = runBlocking {
        assertNull(filtersApi.selected())
    }

    @Test
    fun aSelectedCustomFilterComesBackWithItsListFiltersId() = runBlocking {
        val id = seedFilter(title = "Due window", sql = " WHERE tasks.dueDate > NOW()")
        setSelection(CustomFilter(db.filterDao().getById(id)!!))

        val selection = filtersApi.selected(includeSql = true)

        assertEquals("custom_filter", selection!!.type)
        assertEquals(id, selection.id)
        assertEquals("Due window", selection.title)
        assertEquals(" WHERE tasks.dueDate > NOW()", selection.sql)
    }

    @Test
    fun aSelectedBuiltInFilterComesBackWithItsKeyNotAnId() = runBlocking {
        koin.get<TasksPreferences>().set(TasksPreferences.lastViewedList, "0:1")

        val selection = filtersApi.selected()

        assertEquals("builtin", selection!!.type)
        assertNull(selection.id)
        assertNull(selection.sql)
        assertEquals("builtin_today", selection.key)
    }

    @Test
    fun aSelectedTagFilterComesBackWithTheTagId() = runBlocking {
        val tagId = db.tagDataDao().insert(TagData(name = "hometag"))
        val tag = db.tagDataDao().getById(tagId)!!
        setSelection(TagFilter(tag))

        val selection = filtersApi.selected()

        assertEquals("tag", selection!!.type)
        assertEquals(tagId, selection.id)
        assertEquals("hometag", selection.title)
        assertNull(selection.key)
    }

    private suspend fun dueWindowFilterId(): Long =
        seedFilter(
            title = "Due window",
            sql = " WHERE tasks.dueDate > NOW() - 86400000 AND tasks.dueDate <= NOW() + 86400000",
        )

    private suspend fun titleFilterId(fragment: String): Long =
        seedFilter(
            title = "Title $fragment",
            sql = " WHERE tasks.title LIKE '%$fragment%' ESCAPE '\\'",
        )

    private suspend fun seedFilter(title: String, sql: String, order: Int = 0): Long =
        db.filterDao().insert(Filter(title = title, sql = sql, order = order))

    private suspend fun seedTask(title: String, dueDate: Long = 0): Long {
        val task = Task(title = title, dueDate = dueDate, remoteId = UUIDHelper.newUUID())
        return db.taskDao().insert(task)
    }

    private suspend fun setSelection(filter: org.tasks.filters.Filter) {
        koin.get<TasksPreferences>()
            .set(TasksPreferences.lastViewedList, koin.get<FilterPreferenceCodec>().encode(filter)!!)
    }

    private companion object {
        const val DAY = 86_400_000L
    }
}
