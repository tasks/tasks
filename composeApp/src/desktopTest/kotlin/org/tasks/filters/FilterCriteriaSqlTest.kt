package org.tasks.filters

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure (no database) tests of the serialized criterion format and the
 * SQL/`values` builders. The expected strings are pinned by hand from the
 * shared SQL builder's rendering rules, so they also guard against drift in
 * the shared primitives.
 */
// Hand-pinned SQL/serialization fixtures: the duplicated literals and the per-criterion
// test methods are deliberate, so the complexity/duplication findings are suppressed.
@Suppress("TooManyFunctions", "LongParameterList", "StringLiteralDuplication")
class FilterCriteriaSqlTest {

    private val activeAndVisible =
        "((tasks.completed<=0) AND (tasks.deleted<=0) AND ((tasks.hideUntil<=(strftime('%s','now')*1000)) OR (tasks.completed>0)))"

    private fun multiSelect(
        identifier: String,
        text: String,
        sql: String?,
        valuesForNewTasks: Map<String, Any>? = emptyMap(),
        entryTitles: Array<out String?>? = arrayOf("a", "b"),
        entryValues: Array<out String?>? = arrayOf("x", "y"),
        name: String? = "Name",
    ) = MultipleSelectCriterion(identifier, text, sql, valuesForNewTasks, entryTitles, entryValues, name)

    private fun instance(
        criterion: CustomFilterCriterion,
        type: Int,
        selectedIndex: Int = -1,
        selectedText: String? = null,
    ) = CriterionInstance().apply {
        this.criterion = criterion
        this.type = type
        this.selectedIndex = selectedIndex
        this.selectedText = selectedText
    }

    @Test
    fun universeRowRendersAsActiveAndVisible() {
        val universe = instance(
            multiSelect("active", "My Tasks", null, null, null, null, null),
            CriterionInstance.TYPE_UNIVERSE,
        )
        assertEquals(" WHERE $activeAndVisible", listOf(universe).sql)
    }

    @Test
    fun intersectRowWrapsCriterionSqlInIdIn() {
        val subtask = instance(
            BooleanCriterion("subtask", "Is subtask", "SELECT _id FROM tasks WHERE ((parent>0)=1)"),
            CriterionInstance.TYPE_INTERSECT,
        )
        assertEquals(
            " WHERE $activeAndVisible AND tasks._id IN (SELECT _id FROM tasks WHERE ((parent>0)=1))",
            listOf(universe(), subtask).sql,
        )
    }

    @Test
    fun addAndSubtractUseOrAndAndNot() {
        val first = instance(
            BooleanCriterion("subtask", "Is subtask", "SELECT _id FROM tasks WHERE ((parent>0)=1)"),
            CriterionInstance.TYPE_INTERSECT,
        )
        val second = instance(
            BooleanCriterion("subtask", "Is subtask", "SELECT _id FROM tasks WHERE ((parent>0)=1)"),
            CriterionInstance.TYPE_ADD,
        )
        val third = instance(
            BooleanCriterion("subtask", "Is subtask", "SELECT _id FROM tasks WHERE ((parent>0)=1)"),
            CriterionInstance.TYPE_SUBTRACT,
        )
        assertEquals(
            " WHERE $activeAndVisible AND tasks._id IN (SELECT _id FROM tasks WHERE ((parent>0)=1))" +
                " OR tasks._id IN (SELECT _id FROM tasks WHERE ((parent>0)=1))" +
                " AND NOT tasks._id IN (SELECT _id FROM tasks WHERE ((parent>0)=1))",
            listOf(universe(), first, second, third).sql,
        )
    }

    @Test
    fun questionMarkIsReplacedWithTheSanitizedEntryValue() {
        val due = instance(
            multiSelect(
                "dueDate",
                "Due By: ?",
                "SELECT _id FROM tasks WHERE (dueDate<=?)",
                mapOf("dueDate" to "?"),
                arrayOf("Today"),
                arrayOf("123"),
            ),
            CriterionInstance.TYPE_INTERSECT,
            selectedIndex = 0,
        )
        assertEquals(
            " WHERE $activeAndVisible AND tasks._id IN (SELECT _id FROM tasks WHERE (dueDate<=123))",
            listOf(universe(), due).sql,
        )
    }

    @Test
    fun valuesComeOnlyFromIntersectRowsAndSubstituteTheEntryValue() {
        val tagged = instance(
            multiSelect(
                "tag_is",
                "Tag: ?",
                "SELECT task FROM tags WHERE (name=?)",
                mapOf("tags-tag" to "?"),
                arrayOf("work"),
                arrayOf("tag-uuid"),
            ),
            CriterionInstance.TYPE_INTERSECT,
            selectedIndex = 0,
        )
        val excluded = instance(
            multiSelect(
                "tag_is",
                "Tag: ?",
                "SELECT task FROM tags WHERE (name=?)",
                mapOf("tags-tag" to "?"),
                arrayOf("home"),
                arrayOf("home-uuid"),
            ),
            CriterionInstance.TYPE_SUBTRACT,
            selectedIndex = 0,
        )
        // SUBTRACT rows contribute no values; the INTERSECT row substitutes its entry.
        assertEquals("tags-tag|stag-uuid|", listOf(universe(), tagged, excluded).values)
    }

    @Test
    fun serializationRoundTrips() {
        val rows = listOf(
            universe(),
            instance(
                multiSelect(
                    "dueDate",
                    "Due By: ?",
                    "SELECT _id FROM tasks WHERE (dueDate<=?)",
                    mapOf("dueDate" to "?"),
                    arrayOf("Today"),
                    arrayOf("123"),
                ),
                CriterionInstance.TYPE_INTERSECT,
                selectedIndex = 0,
            ),
            instance(
                TextInputCriterion("title", "Title contains: ?", "SELECT _id FROM tasks WHERE (title LIKE ?)", "p", "", "t"),
                CriterionInstance.TYPE_ADD,
                selectedText = "hello",
            ),
            instance(
                BooleanCriterion("subtask", "Is subtask", "SELECT _id FROM tasks WHERE ((parent>0)=1)"),
                CriterionInstance.TYPE_SUBTRACT,
            ),
        )
        val serialized = CriterionInstance.serialize(rows)
        val rehydrated = rehydrate(serialized, rows.map { it.criterion })
        assertEquals(rows.size, rehydrated.size)
        for (i in rows.indices) {
            // Row ids are not part of the serialized format, so compare the rest.
            assertEquals(rows[i].criterion, rehydrated[i].criterion)
            assertEquals(rows[i].selectedIndex, rehydrated[i].selectedIndex)
            assertEquals(rows[i].selectedText, rehydrated[i].selectedText)
            assertEquals(rows[i].type, rehydrated[i].type)
        }
    }

    @Test
    fun pipesInTextAreEscaped() {
        val row = instance(
            TextInputCriterion("title", "Title contains: ?", "SELECT _id FROM tasks", "p", "", "t"),
            CriterionInstance.TYPE_INTERSECT,
            selectedText = "a|b",
        )
        val serialized = CriterionInstance.serialize(listOf(row))
        assertEquals("title|a!PIPE!b|Title contains: ?|2|SELECT _id FROM tasks", serialized)
        // Round-trip through the provider's unescape logic (same format as Android).
        val split = serialized
            .split("|")
            .map { it.replace("!PIPE!", "|") }
        assertEquals(listOf("title", "a|b", "Title contains: ?", "2", "SELECT _id FROM tasks"), split)
    }

    private fun universe() = instance(
        multiSelect("active", "My Tasks", null, null, null, null, null),
        CriterionInstance.TYPE_UNIVERSE,
    )

    // Minimal mirror of `FilterCriteriaProvider.fromString`'s row parsing for
    // the criterion instances under test (avoids a database in this class).
    private fun rehydrate(serialized: String, criteria: List<CustomFilterCriterion>): List<CriterionInstance> {
        val entries = mutableListOf<CriterionInstance>()
        for (row in serialized.trim().split("\n")) {
            val split = row.split("|").map { it.replace("!PIPE!", "|") }
            val entry = CriterionInstance()
            val c = criteria[entries.size]
            entry.criterion = c
            val value = split[1]
            when (c) {
                is TextInputCriterion -> entry.selectedText = value
                is MultipleSelectCriterion ->
                    entry.selectedIndex = c.entryValues?.indexOf(value) ?: -1
                else -> Unit
            }
            entry.type = split[3].toInt()
            entries.add(entry)
        }
        return entries
    }
}
