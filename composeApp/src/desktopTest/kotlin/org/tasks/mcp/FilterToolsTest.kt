package org.tasks.mcp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import org.tasks.api.FilterQuery
import org.tasks.api.TagWrite
import org.tasks.data.UUIDHelper
import org.tasks.data.entity.Filter
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.entity.TaskListMetadata
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
    fun theFilterToolsJoinTheCatalogInTheirOwnGroup() {
        val filters = McpToolCatalog.tools.filter { it.group == McpToolGroup.Filters }

        assertEquals(
            listOf(
                "list_filters", "get_filter_tasks", "get_selected_filter",
                "list_filter_criteria", "create_filter", "update_filter", "delete_filter",
            ),
            filters.map { it.name },
        )
        listOf("list_filters", "get_filter_tasks", "get_selected_filter", "list_filter_criteria")
            .forEach { name ->
                assertTrue("$name must be a read tool", name in McpToolCatalog.readTools)
                assertTrue(
                    "$name must be allowed in read-only mode",
                    ToolPermissions.of(AccessMode.ReadOnly).allows(name),
                )
            }
        listOf("create_filter", "update_filter", "delete_filter").forEach { name ->
            assertFalse("$name must not be a read tool", name in McpToolCatalog.readTools)
            assertFalse(
                "$name must be refused in read-only mode",
                ToolPermissions.of(AccessMode.ReadOnly).allows(name),
            )
            assertTrue(
                "$name must be allowed in read-write mode",
                ToolPermissions.of(AccessMode.ReadWrite).allows(name),
            )
        }
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

    @Test
    fun createRoundTripsThroughTheAppSerializers() = runBlocking {
        val tagId = api.createTag(TagWrite(name = "hometag")).row.id
        val tagged = seedTask(title = "Heph home task")
        api.setTaskTags(listOf(tagged), listOf(tagId), emptyList())
        seedTask(title = "Other task")

        val row = filtersApi.create(
            title = "Home things",
            icon = null,
            color = null,
            criteria = listOf(
                DatabaseFiltersApi.CriterionWrite(identifier = "tag_is", value = "hometag"),
                DatabaseFiltersApi.CriterionWrite(identifier = "title", value = "Heph"),
            ),
        )

        // list_filters hands back the structured criteria exactly as sent, plus the
        // app-managed leading universe row.
        val listed = filtersApi.list(ids = listOf(row.id)).rows.single()
        assertEquals("Home things", listed.title)
        assertEquals("the universe row is stored leading", "universe", listed.criteria.first().type)
        val sent = listed.criteria.filter { it.type != "universe" }
        assertEquals(listOf("tag_is", "title"), sent.map { it.identifier })
        assertEquals(listOf("hometag", "Heph"), sent.map { it.value })

        // and the rendered SQL is executable: get_filter_tasks returns exactly the match.
        val page = filtersApi.tasks(row.id, FilterQuery())
        assertEquals(listOf("Heph home task"), page.rows.map { it.title })
    }

    @Test
    fun aTextValueWithAQuoteSurvivesSanitisation() = runBlocking {
        seedTask(title = "don't do this")
        seedTask(title = "do it")

        val row = filtersApi.create(
            title = "Quotes",
            icon = null,
            color = null,
            criteria = listOf(DatabaseFiltersApi.CriterionWrite(identifier = "title", value = "don't")),
        )

        val page = filtersApi.tasks(row.id, FilterQuery())
        assertEquals("the quoted value must match the quoted task and nothing else",
            listOf("don't do this"), page.rows.map { it.title })
    }

    @Test
    fun anInvalidMultiselectValueIsRejectedWithTheValidValues() = runBlocking {
        api.createTag(TagWrite(name = "hometag"))

        val error = runCatching {
            filtersApi.create(
                title = "Broken",
                icon = null,
                color = null,
                criteria = listOf(DatabaseFiltersApi.CriterionWrite(identifier = "tag_is", value = "nowhere")),
            )
        }.exceptionOrNull()

        assertNotNull(error)
        assertTrue("the error names the offending value", error!!.message!!.contains("nowhere"))
        assertTrue("the error lists the valid values", error.message!!.contains("hometag"))
        assertEquals("nothing was written", 0, filtersApi.list().total)
    }

    @Test
    fun anUnknownIdentifierIsRejectedWithTheCatalogIdentifiers() = runBlocking {
        val error = runCatching {
            filtersApi.create(
                title = "Broken",
                icon = null,
                color = null,
                criteria = listOf(DatabaseFiltersApi.CriterionWrite(identifier = "no_such_criterion")),
            )
        }.exceptionOrNull()

        assertNotNull(error)
        val message = error!!.message!!
        assertTrue(message.contains("'no_such_criterion'"))
        assertTrue("the error lists valid identifiers", message.contains("valid identifiers:"))
        // and the listed identifiers agree with the live catalog
        val catalog = filtersApi.criteriaCatalog().map { it.identifier }.toSet()
        assertTrue(catalog.containsAll(listOf("title", "tag_is")))
        catalog.forEach { assertTrue("$it must be in the error", message.contains(it)) }
    }

    @Test
    fun aBooleanCriterionTakesNoValueAndABooleanWithOneIsRejected() = runBlocking {
        val ok = filtersApi.create(
            title = "Open",
            icon = null,
            color = null,
            criteria = listOf(DatabaseFiltersApi.CriterionWrite(identifier = "completed")),
        )
        assertEquals(1, filtersApi.list(ids = listOf(ok.id)).rows.single().criteria.count { it.type != "universe" })

        val error = runCatching {
            filtersApi.create(
                title = "Broken",
                icon = null,
                color = null,
                criteria = listOf(DatabaseFiltersApi.CriterionWrite(identifier = "completed", value = "yes")),
            )
        }.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error!!.message!!.contains("boolean and takes no value"))
    }

    @Test
    fun aBlankTitleAndEmptyCriteriaAreRejected() = runBlocking {
        val blankTitle = runCatching {
            filtersApi.create("  ", null, null, listOf(DatabaseFiltersApi.CriterionWrite("completed")))
        }.exceptionOrNull()
        assertNotNull(blankTitle)

        val emptyCriteria = runCatching {
            filtersApi.create("No rows", null, null, emptyList())
        }.exceptionOrNull()
        assertNotNull(emptyCriteria)
        assertTrue(emptyCriteria!!.message!!.contains("must not be empty"))

        assertEquals("neither write landed", 0, filtersApi.list().total)
    }

    @Test
    fun updatePatchesOnlyWhatIsSent() = runBlocking {
        val seed = filtersApi.create(
            title = "Original",
            icon = null,
            color = 0xFF000000.toInt(),
            criteria = listOf(DatabaseFiltersApi.CriterionWrite(identifier = "completed")),
        )

        val patched = filtersApi.update(id = seed.id, title = "Renamed", icon = null, color = null, criteria = null)

        assertEquals("the id survives the patch", seed.id, patched.id)
        assertEquals("Renamed", patched.title)
        assertEquals("untouched fields keep their values", 0xFF000000.toInt(), patched.color)
        assertEquals(seed.criteria, patched.criteria)
    }

    @Test
    fun updateReplacesCriteriaUnderTheSameId() = runBlocking {
        val seedTaskId = seedTask(title = "Heph thing")
        val otherTaskId = seedTask(title = "Someone else's thing")
        val seed = filtersApi.create(
            title = "Heph only",
            icon = null,
            color = null,
            criteria = listOf(DatabaseFiltersApi.CriterionWrite(identifier = "title", value = "Heph")),
        )
        assertEquals(listOf("Heph thing"), filtersApi.tasks(seed.id, FilterQuery()).rows.map { it.title })

        val updated = filtersApi.update(
            id = seed.id,
            title = null,
            icon = null,
            color = null,
            criteria = listOf(DatabaseFiltersApi.CriterionWrite(identifier = "title", value = "thing")),
        )

        assertEquals("the pin-relevant id is preserved", seed.id, updated.id)
        val page = filtersApi.tasks(updated.id, FilterQuery())
        assertEquals(
            "the new criteria apply under the same id",
            setOf(seedTaskId, otherTaskId),
            page.rows.map { it.id }.toSet(),
        )
    }

    @Test
    fun anEmptyPatchAndAnUnknownFilterIdAreCleanErrors() = runBlocking {
        val emptyPatch = runCatching {
            filtersApi.update(999, null, null, null, null)
        }.exceptionOrNull()
        assertNotNull(emptyPatch)
        assertTrue(emptyPatch!!.message!!.contains("No filter with id 999"))

        val seed = filtersApi.create("Has a title", null, null, listOf(DatabaseFiltersApi.CriterionWrite("completed")))
        val nothing = runCatching {
            filtersApi.update(seed.id, null, null, null, null)
        }.exceptionOrNull()
        assertNotNull(nothing)
        assertTrue(nothing!!.message!!.contains("Nothing to change"))
    }

    @Test
    fun deleteRemovesTheRowAndASecondCallIsACleanError() = runBlocking {
        val seed = filtersApi.create("Doomed", null, null, listOf(DatabaseFiltersApi.CriterionWrite("completed")))

        val result = filtersApi.delete(seed.id)
        assertEquals(seed.id, result.id)
        assertTrue(result.pinnedBy.isEmpty())

        assertEquals("the row is gone", 0, filtersApi.list().total)
        val again = runCatching { filtersApi.delete(seed.id) }.exceptionOrNull()
        assertNotNull(again)
        assertTrue(again!!.message!!.contains("No filter with id ${seed.id}"))
    }

    @Test
    fun deleteReportsPinnedLists() = runBlocking {
        val seed = filtersApi.create("Pinned", null, null, listOf(DatabaseFiltersApi.CriterionWrite("completed")))
        val pinnedList = db.taskListMetadataDao().insert(
            TaskListMetadata().apply { tagUuid = Task.NO_UUID; filter = seed.id.toString() },
        )
        db.taskListMetadataDao().insert(TaskListMetadata())

        val result = filtersApi.delete(seed.id)

        assertEquals(listOf(pinnedList), result.pinnedBy)
        assertEquals("only the filter row is removed - lists survive", 2, db.taskListMetadataDao().getAll().size)
    }

    @Test
    fun theCriteriaCatalogMatchesTheLiveDatabase() = runBlocking {
        val before = filtersApi.criteriaCatalog().first { it.identifier == "tag_is" }
        assertNull("no tags seeded yet", before.values?.takeIf { it.isNotEmpty() })

        db.tagDataDao().insert(TagData(name = "catalogtag"))
        val after = filtersApi.criteriaCatalog().first { it.identifier == "tag_is" }

        assertEquals("tag options are built from the live tag table", listOf("catalogtag"), after.values?.map { it.value })
        assertEquals("multi_select is the kind name", "multi_select", after.kind)
        assertEquals("boolean is the kind name", "boolean", filtersApi.criteriaCatalog().first { it.identifier == "completed" }.kind)
        assertEquals("text_input is the kind name", "text_input", filtersApi.criteriaCatalog().first { it.identifier == "title" }.kind)
        assertFalse("the universe row is app-managed and absent from the catalog",
            filtersApi.criteriaCatalog().any { it.identifier == "active" })
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
