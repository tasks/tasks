package org.tasks.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterWhereTest {

    @Test
    fun stripsTheStoredWherePrefix() {
        assertEquals(DUE_PREDICATE, filterWhere(" WHERE tasks.dueDate > 0"))
    }

    @Test
    fun isLenientAboutWhitespaceAndCase() {
        assertEquals(DUE_PREDICATE, filterWhere("where  tasks.dueDate > 0"))
        assertEquals(DUE_PREDICATE, filterWhere("\n WHERE tasks.dueDate > 0 "))
    }

    @Test
    fun keepsThePredicateIntactWhenItContainsOrs() {
        val stored = " WHERE (tasks._id IN (SELECT task_id FROM for_tags)) OR (tasks.importance = 2)"
        assertEquals("(tasks._id IN (SELECT task_id FROM for_tags)) OR (tasks.importance = 2)", filterWhere(stored))
    }

    @Test
    fun replacesPermaSqlPlaceholders() {
        val before = System.currentTimeMillis()
        val condition = filterWhere(" WHERE tasks.dueDate > NOW()")
        assertTrue("no NOW() left: $condition", !condition.contains("NOW()"))
        val value = condition.removePrefix("tasks.dueDate > ").toLong()
        assertTrue(value >= before)
        assertTrue("placeholder resolved far in the past: $value", value <= System.currentTimeMillis())
    }

    @Test
    fun rejectsSqlWithoutAWhere() {
        val error = runCatching { filterWhere(DUE_PREDICATE) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(error!!.message!!.contains("WHERE"))
    }

    @Test
    fun rejectsAnEmptyWhere() {
        val error = runCatching { filterWhere(" WHERE   ") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(error!!.message!!.contains("empty"))
    }

    private companion object {
        const val DUE_PREDICATE = "tasks.dueDate > 0"
    }
}
