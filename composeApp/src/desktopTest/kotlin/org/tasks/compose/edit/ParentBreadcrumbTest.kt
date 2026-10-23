package org.tasks.compose.edit

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Task

private fun task(id: Long, parent: Long, title: String = "T$id") =
    Task().apply {
        this.id = id
        this.parent = parent
        this.title = title
    }

class ParentBreadcrumbTest {

    /** Resolves the chain for [ids] (map id -> parent), starting at [startId]. */
    private suspend fun chain(
        idToParent: Map<Long, Long>,
        startId: Long,
        excludeId: Long,
        maxDepth: Int = MAX_PARENT_CHAIN_DEPTH,
    ): List<Task> {
        val tasks = idToParent.keys.associateWith { task(it, idToParent[it] ?: 0L) }
        return fetchParentChain(
            fetch = { tasks[it] },
            startId = startId,
            excludeId = excludeId,
            maxDepth = maxDepth,
        )
    }

    @Test
    fun chainOfThreeLevelsResolvesOldestFirst() = runTest {
        // task 10 (exclude) <- 3 <- 2 <- 1 (top level)
        val result = chain(mapOf(3L to 2L, 2L to 1L, 1L to 0L), startId = 3L, excludeId = 10L)
        assertEquals(listOf(1L, 2L, 3L), result.map(Task::id))
    }

    @Test
    fun directParentOnly() = runTest {
        val result = chain(mapOf(5L to 0L), startId = 5L, excludeId = 9L)
        assertEquals(listOf(5L), result.map(Task::id))
    }

    @Test
    fun stopsWhenParentRowMissing() = runTest {
        // 7's parent is 6, but 6 is not present (calendar not synced)
        val result = chain(mapOf(7L to 6L), startId = 7L, excludeId = 99L)
        assertEquals(listOf(7L), result.map(Task::id))
    }

    @Test
    fun stopsAtCycle() = runTest {
        // 1 <-> 2 cycle, task 9's parent is 1
        val result = chain(mapOf(1L to 2L, 2L to 1L), startId = 1L, excludeId = 9L)
        // traversal hit 1 first, then 2, then the cycle; reversed for oldest-first output
        assertEquals(listOf(2L, 1L), result.map(Task::id))
    }

    @Test
    fun selfReferencingParentReturnsEmpty() = runTest {
        // task 4's parent is itself
        val result = chain(emptyMap(), startId = 4L, excludeId = 4L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun depthCapStopsAtMaxDepth() = runTest {
        // 15 deep chain: 15 -> 14 -> ... -> 1 -> 0
        val idToParent = (1L..15L).associate { it to (if (it == 1L) 0L else it - 1L) }
        val result = chain(idToParent, startId = 15L, excludeId = 999L)
        assertEquals(MAX_PARENT_CHAIN_DEPTH, result.size)
        // oldest-first: the deepest ancestor that still fits is id 6 (15..6 = 10 rows)
        assertEquals(6L, result.first().id)
        assertEquals(15L, result.last().id)
    }
}
