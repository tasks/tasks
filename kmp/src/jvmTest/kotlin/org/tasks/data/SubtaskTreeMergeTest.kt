package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Task

class SubtaskTreeMergeTest {
    private val trees = SubtaskTrees()
    private val root = "root"
    private val rootId = 42L

    private fun row(
        id: Long,
        title: String,
        parent: Long = rootId,
        indent: Int = 0,
        completed: Boolean = false,
    ) =
        TaskContainer(
            task = Task(
                id = id,
                title = title,
                parent = parent,
                remoteId = "uuid-$title",
                completionDate = if (completed) COMPLETED_AT else 0,
            ),
            indent = indent,
        )

    private fun merge(vararg rows: TaskContainer) = trees.merge(root, rootId, rows.toList())

    private fun tree(): List<String> =
        trees.rowsOf(root).map { "${"  ".repeat(it.indent)}${it.node.title}" }

    private fun key(title: String) = "uuid-$title"

    private fun completions(): List<Boolean> = trees.rowsOf(root).map { it.completed }

    @Test
    fun buildTheTreeTheQueryDescribes() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1), row(3, "c"))

        assertEquals(listOf("a", "  b", "c"), tree())
    }

    @Test
    fun keepAnArrangementWhenTheSameRowsComeBack() {
        merge(row(1, "a"), row(2, "b"), row(3, "c"))
        trees.move(key = key("c"), parentKey = root, after = null)

        merge(row(1, "a"), row(2, "b"), row(3, "c"))

        assertEquals(listOf("c", "a", "b"), tree())
    }

    @Test
    fun keepAnArrangementEvenWhenTheRowsHaveMovedUnderneathIt() {
        merge(row(1, "a"), row(2, "b"))
        trees.indent(key("b"))

        merge(row(1, "a"), row(2, "b"))

        assertEquals(listOf("a", "  b"), tree())
    }

    @Test
    fun refuseAReparentingThatWouldInvertAStagedNesting() {
        merge(row(1, "a"), row(2, "b"))
        trees.indent(key("b"))

        merge(row(2, "b"), row(1, "a", parent = 2, indent = 1))

        assertEquals(listOf("a", "  b"), tree())
        assertEquals(setOf(key("a"), key("b")), trees.nodes.value.keys)
    }

    @Test
    fun takeContentsFromTheRow() {
        merge(row(1, "a"))

        merge(row(1, "renamed"))

        assertEquals(listOf("renamed"), tree())
    }

    @Test
    fun keepAStagedCompletion() {
        merge(row(1, "a"))
        trees.setCompleted(key("a"), true)

        merge(row(1, "a"))

        assertTrue(trees.get(key("a"))!!.completed)
        assertTrue(trees.get(key("a"))!!.completionEdited)
    }

    @Test
    fun takeCompletionFromTheRow() {
        merge(row(1, "a"))
        assertFalse(trees.get(key("a"))!!.completed)

        merge(row(1, "a", completed = true))

        assertTrue(trees.get(key("a"))!!.completed)
        assertFalse(trees.get(key("a"))!!.completionEdited)
    }

    @Test
    fun aSubtaskUnTickedOnItsOwnAccountStaysThatWayWhenWhatIsInsideItComesBack() {
        merge(
            row(1, "a", completed = true),
            row(2, "b", parent = 1, indent = 1, completed = true),
        )

        trees.setCompleted(key("a"), false)
        trees.setCompleted(key("b"), true)

        assertEquals(listOf(false, true), completions())
    }

    @Test
    fun leaveSubtasksThatHaveNoRowAlone() {
        merge(row(1, "a"))
        val added = trees.add(root, Task(title = "new", remoteId = "uuid-new"), list = null)

        merge(row(1, "a"))

        assertEquals(listOf("a", "new"), tree())
        assertNull(trees.get(added.key)?.id?.takeIf { it > 0 })
    }

    @Test
    fun dropARowThatHasGoneAndKeepWhatWasNestedInIt() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))

        merge(row(2, "b"))

        assertEquals(listOf("b"), tree())
    }

    @Test
    fun keepWhatWasNestedUnderARowWhoseOwnParentGoesWithIt() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1), row(3, "c"))
        trees.move(key = key("c"), parentKey = key("b"), after = null)
        assertEquals(listOf("a", "  b", "    c"), tree())

        merge(row(3, "c"))

        assertEquals(listOf("c"), tree())
        assertEquals(root, trees.get(key("c"))!!.parentKey)
    }

    @Test
    fun dropEveryRowWhenTheQueryComesBackWithNothing() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))

        merge()

        assertEquals(emptyList<String>(), tree())
    }

    @Test
    fun leaveSubtasksThatHaveNoRowAloneWhenTheQueryComesBackWithNothing() {
        val added = trees.add(root, Task(title = "new", remoteId = "uuid-new"), list = null)

        merge()

        assertEquals(listOf("new"), tree())
        assertNotNull(trees.get(added.key))
    }

    @Test
    fun aSubtaskStagedUnderAnotherSurvivesThatOnesOwnEditorRefreshing() {
        merge(row(1, "a"), row(2, "b"))
        trees.indent(key("b"))
        assertEquals(listOf("a", "  b"), tree())

        trees.merge(key("a"), 1, emptyList())

        assertEquals(listOf("a", "  b"), tree())
    }

    @Test
    fun aSubtaskStagedUnderAnotherIsStillDroppedWhenItsOwnRowGoes() {
        merge(row(1, "a"), row(2, "b"))
        trees.indent(key("b"))

        merge(row(1, "a"))

        assertEquals(listOf("a"), tree())
    }

    @Test
    fun addARowThatHasAppearedUnderTheParentTheDatabaseGivesIt() {
        merge(row(1, "a"))

        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))

        assertEquals(listOf("a", "  b"), tree())
    }

    @Test
    fun mergingIsNotARearrangement() {
        merge(row(1, "a"), row(2, "b"))

        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun aRowThatAppearsAtTheTopOfTheRunIsDrawnAtTheTop() {
        merge(row(1, "a"), row(2, "b"))

        merge(row(3, "c"), row(1, "a"), row(2, "b"))

        assertEquals(listOf("c", "a", "b"), tree())
    }

    @Test
    fun aRowThatAppearsInTheMiddleOfTheRunIsDrawnThere() {
        merge(row(1, "a"), row(2, "b"))

        merge(row(1, "a"), row(3, "c"), row(2, "b"))

        assertEquals(listOf("a", "c", "b"), tree())
    }

    @Test
    fun anArrivalGoesBelowEverythingTheQueryDrewAboveIt() {
        merge(row(1, "a"), row(2, "b"))
        trees.move(key = key("b"), parentKey = root, after = null)
        assertEquals(listOf("b", "a"), tree())

        merge(row(1, "a"), row(2, "b"), row(3, "c"))

        assertEquals(listOf("b", "a", "c"), tree())
    }

    @Test
    fun unTickingSomethingTheDatabaseHasDownAsFinishedStillCascades() {
        merge(
            row(1, "a", completed = true),
            row(2, "b", parent = 1, indent = 1, completed = true),
        )

        trees.setCompleted(key("a"), false)

        assertEquals(listOf(false, false), completions())
        assertFalse(trees.get(key("b"))!!.completionEdited)
    }

    @Test
    fun aReparentingThatArrivesFromTheDatabaseIsTakenUp() {
        merge(row(1, "a"), row(2, "b"))

        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))

        assertEquals(listOf("a", "  b"), tree())
        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun aReorderingThatArrivesFromTheDatabaseIsTakenUp() {
        merge(row(1, "a"), row(2, "b"), row(3, "c"))

        merge(row(3, "c"), row(1, "a"), row(2, "b"))

        assertEquals(listOf("c", "a", "b"), tree())
        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun aReorderingThatArrivesIsRefusedWhereTheUserHasDraggedTheRun() {
        merge(row(1, "a"), row(2, "b"), row(3, "c"))
        trees.move(key = key("c"), parentKey = root, after = null)

        merge(row(1, "a"), row(2, "b"), row(3, "c"))

        assertEquals(listOf("c", "a", "b"), tree())
    }

    @Test
    fun anUnsavedSubtaskKeepsItsRunOutOfTheQuerysHands() {
        merge(row(1, "a"), row(2, "b"))
        trees.add(root, Task(title = "new", remoteId = "uuid-new"), list = null)

        merge(row(2, "b"), row(1, "a"))

        assertEquals(listOf("a", "b", "new"), tree())
    }

    private fun clearWrittenLeaves(written: List<SubtaskNode>): Boolean {
        trees.clearWritten(written)
        return written.none { trees.get(it.key) != null }
    }

    private fun TaskContainer.collapsed(): TaskContainer =
        copy(task = task.copy(isCollapsed = true))

    companion object {
        private const val COMPLETED_AT = 1_700_000_000_000L
    }
}
