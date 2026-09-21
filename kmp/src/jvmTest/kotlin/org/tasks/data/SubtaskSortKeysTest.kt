package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.entity.Task

class SubtaskSortKeysTest {
    private val parentId = 7L

    private fun run(vararg orders: Long?, parent: Long = parentId) =
        orders.map { Task(parent = parent, order = it) }

    private fun changed(rows: List<Task?>, keys: List<Long>) =
        rows.indices.count { rows[it]?.order != keys[it] }

    @Test
    fun aRunThatIsAlreadyInOrderIsLeftExactlyAsItIs() {
        val rows = run(700_000_000L, 700_001_000L, 700_002_000L)

        val keys = sortKeysFor(rows, parentId)

        assertEquals(listOf(700_000_000L, 700_001_000L, 700_002_000L), keys)
        assertEquals(0, changed(rows, keys))
    }

    @Test
    fun aRowMovedWithinASparseRunIsTheOnlyOneRewritten() {
        val rows = run(
            700_000_000L, 700_003_000L, 700_001_000L, 700_002_000L, 700_004_000L,
        )

        val keys = sortKeysFor(rows, parentId)

        assertEquals(1, changed(rows, keys))
        assertEquals(700_000_500L, keys[1])
        assertIncreasing(keys)
    }

    @Test
    fun theRowThatMovedDoesNotBecomeTheAnchorJustBecauseItsKeyIsLarger() {
        val rows = run(
            700_000_000L, 700_003_000L, 700_001_000L, 700_002_000L, 700_004_000L,
        )

        val keys = sortKeysFor(rows, parentId)

        assertEquals(700_000_000L, keys[0])
        assertEquals(700_001_000L, keys[2])
        assertEquals(700_002_000L, keys[3])
        assertEquals(700_004_000L, keys[4])
    }

    @Test
    fun aDenselyNumberedRunFallsBackToNumberingItselfAgain() {
        val rows = run(1L, 0L, 2L)

        val keys = sortKeysFor(rows, parentId)

        assertEquals(listOf(0L, 1L, 2L), keys)
        assertEquals(2, changed(rows, keys))
    }

    @Test
    fun aRowWithNoKeyAtAllIsGivenOne() {
        val rows = run(700_000_000L, null, 700_001_000L)

        val keys = sortKeysFor(rows, parentId)

        assertEquals(1, changed(rows, keys))
        assertEquals(700_000_500L, keys[1])
    }

    @Test
    fun aRunNothingHasEverPositionedIsNumberedFromZero() {
        val rows = run(null, null, null)

        assertEquals(listOf(0L, 1L, 2L), sortKeysFor(rows, parentId))
    }

    @Test
    fun aNewRowAtTheHeadOfTheRunGoesInFrontOfWhatIsThere() {
        val rows = run(null, 700_000_000L, 700_001_000L)

        val keys = sortKeysFor(rows, parentId)

        assertEquals(699_999_999L, keys[0])
        assertEquals(1, changed(rows, keys))
    }

    @Test
    fun aNewRowAtTheEndOfTheRunGoesAfterIt() {
        val rows = run(700_000_000L, 700_001_000L, null)

        val keys = sortKeysFor(rows, parentId)

        assertEquals(700_001_001L, keys[2])
        assertEquals(1, changed(rows, keys))
    }

    @Test
    fun severalNewRowsInOneGapAreSpreadThroughIt() {
        val rows = run(1_000L, null, null, null, 2_000L)

        val keys = sortKeysFor(rows, parentId)

        assertEquals(listOf(1_000L, 1_250L, 1_500L, 1_750L, 2_000L), keys)
        assertIncreasing(keys)
    }

    @Test
    fun aRowArrivingFromAnotherRunIsAlwaysGivenANewKey() {
        val rows = listOf(
            Task(parent = parentId, order = 1_000L),
            Task(parent = 99L, order = 1_500L),
            Task(parent = parentId, order = 2_000L),
        )

        val keys = sortKeysFor(rows, parentId)

        assertEquals(listOf(1_000L, 1_500L, 2_000L), keys)
        assertEquals(0, changed(rows.take(1), keys.take(1)))
    }

    @Test
    fun aRowTheFetchCouldNotFindStillGetsAPosition() {
        val rows = listOf(Task(parent = parentId, order = 1_000L), null)

        val keys = sortKeysFor(rows, parentId)

        assertEquals(listOf(1_000L, 1_001L), keys)
    }

    @Test
    fun anEmptyRunAsksForNothing() {
        assertEquals(emptyList<Long>(), sortKeysFor(emptyList(), parentId))
    }

    @Test
    fun theKeysAreAlwaysStrictlyIncreasing() {
        listOf(
            run(3L, 1L, 2L),
            run(null, 5L, 1L, null),
            run(0L, 1L, 2L, 3L, 4L),
            run(10L, null, 10L),
            run(Long.MAX_VALUE, 1L),
            run(2L, Long.MIN_VALUE),
        ).forEach { rows ->
            val keys = sortKeysFor(rows, parentId)
            assertEquals(rows.size, keys.size)
            assertIncreasing(keys, "for ${rows.map { it?.order }}")
        }
    }

    @Test
    fun theLongestRunIsTheOneThatLeavesTheLeastToRewrite() {
        assertEquals(listOf(0, 1, 2), listOf<Long?>(1L, 2L, 3L).longestRun().sorted())
        assertEquals(listOf(0, 2, 3), listOf<Long?>(1L, 9L, 2L, 3L).longestRun().sorted())
        assertEquals(emptyList<Int>(), listOf<Long?>(null, null).longestRun())
        assertEquals(emptyList<Int>(), emptyList<Long?>().longestRun())
        assertEquals(1, listOf<Long?>(5L, 5L).longestRun().size)
    }

    @Test
    fun aRunWithNoRoomBelowTheKeyItStartsWithIsNumberedFromScratch() {
        val rows = run(null, Long.MIN_VALUE)

        val keys = sortKeysFor(rows, parentId)

        assertEquals(listOf(0L, 1L), keys)
        assertIncreasing(keys)
    }

    @Test
    fun aRunIsNumberedDownFromAKeeperWhereThereIsRoomToDoIt() {
        val rows = run(null, 0L)

        val keys = sortKeysFor(rows, parentId)

        assertEquals(listOf(-1L, 0L), keys)
        assertEquals(1, changed(rows, keys))
    }

    private fun assertIncreasing(keys: List<Long>, message: String = "") {
        assertEquals(message, keys.sorted(), keys)
        assertEquals(message, keys.size, keys.distinct().size)
    }
}
