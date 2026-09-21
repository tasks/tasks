package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Task

class SubtaskTreeReadsTest {
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

    private fun remaining(title: String): Int =
        trees.rowsOf(root).first { it.node.title == title }.remaining

    private fun children(title: String): Int =
        trees.rowsOf(root).first { it.node.title == title }.children

    @Test
    fun theChipCountsWhatIsLeftUnderASubtask() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c", parent = 1, indent = 1, completed = true),
        )

        assertEquals(1, remaining("a"))
        assertEquals(2, children("a"))
    }

    @Test
    fun theChipCountsWhatIsLeftAtEveryDepth() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c", parent = 2, indent = 2, completed = true),
        )

        assertEquals(1, remaining("a"))
        assertEquals(2, children("a"))
    }

    @Test
    fun aTickedSubtaskHoldingUnfinishedWorkIsItselfStillLeftToDo() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1, completed = true),
            row(3, "c", parent = 2, indent = 2),
        )

        assertEquals(2, remaining("a"))
    }

    @Test
    fun aFinishedSubtaskCountsWhatIsUnderItInstead() {
        merge(
            row(1, "a", completed = true),
            row(2, "b", parent = 1, indent = 1, completed = true),
            row(3, "c", parent = 2, indent = 2, completed = true),
        )

        val row = trees.rowsOf(root).first { it.node.title == "a" }

        assertEquals(2, row.chipCount)
        assertEquals(0, row.remaining)
    }

    @Test
    fun aSubtaskOnItsWayOutIsNotLeftToDo() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))

        trees.delete(key("b"))

        assertEquals(0, remaining("a"))
    }

    @Test
    fun aDeletedSubtaskIsStillShownButWhatIsInsideItIsNot() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        val added = trees.add(key("a"), Task(title = "new", remoteId = "uuid-new"), list = null)

        trees.delete(key("a"))

        assertEquals(listOf("a"), trees.rowsOf(root).visible().map { it.node.title })
        assertEquals(listOf("a", "  b", "  new"), tree())
        assertTrue(trees.get(key("a"))!!.deleted)
        assertFalse(trees.get(key("b"))!!.deleted)
        assertFalse(trees.get(added.key)!!.deleted)
        assertTrue(trees.isRearranged(root))
    }

    @Test
    fun everythingUnderADeletedSubtaskIsOnItsWayOutWithIt() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c", parent = 2, indent = 2),
            row(4, "d"),
        )

        trees.delete(key("a"))

        assertEquals(
            setOf(key("a"), key("b"), key("c")),
            trees.rowsOf(root).doomed(),
        )
    }

    @Test
    fun aSingleLevelListIsStillDrawnWithItsNesting() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c", parent = 2, indent = 2),
        )

        assertEquals(listOf("a", "  b", "    c"), tree())
        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun foldAwayWhatIsInsideACollapsedSubtask() {
        merge(
            row(1, "a").collapsed(),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c"),
        )

        assertEquals(listOf("a", "  b", "c"), tree())
        assertEquals(listOf("a", "c"), trees.rowsOf(root).visible().map { it.node.title })
    }

    @Test
    fun foldAwayWhatIsInsideACollapsedSubtaskOfACollapsedSubtask() {
        merge(
            row(1, "a").collapsed(),
            row(2, "b", parent = 1, indent = 1).collapsed(),
            row(3, "c", parent = 2, indent = 2),
            row(4, "d"),
        )

        assertEquals(listOf("a", "d"), trees.rowsOf(root).visible().map { it.node.title })
    }

    @Test
    fun nestingIsStillThereWhenTheSubtaskHoldingItIsCollapsed() {
        merge(
            row(1, "a").collapsed(),
            row(2, "b", parent = 1, indent = 1),
        )

        assertEquals(listOf("a"), trees.rowsOf(root).visible().map { it.node.title })
        assertTrue(trees.rowsOf(root).nested())
    }

    @Test
    fun aFlatTreeIsNotNested() {
        merge(row(1, "a"), row(2, "b"))

        assertFalse(trees.rowsOf(root).nested())
    }

    @Test
    fun nestingOnItsWayOutDoesNotCount() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))

        trees.delete(key("a"))

        assertFalse(trees.rowsOf(root).nested())
    }

    @Test
    fun countWhatIsNestedInsideEachSubtask() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c", parent = 2, indent = 2),
            row(4, "d"),
        )

        assertEquals(listOf(2, 1, 0, 0), trees.rowsOf(root).map { it.children })
    }

    @Test
    fun aSubtaskOfAnOpenEditorIsRecognisedFromTheTaskList() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))

        assertTrue(trees.holds(1, "uuid-a"))
        assertTrue(trees.holds(2, "uuid-b"))
        assertFalse(trees.holds(rootId, root))
        assertFalse(trees.holds(99, "uuid-elsewhere"))
    }

    @Test
    fun aSubtaskIsOnlyRecognisedWhileItsEditorIsOpen() {
        merge(row(1, "a"))
        trees.clear()

        assertFalse(trees.holds(1, "uuid-a"))
    }

    @Test
    fun aRowTooOldForAUuidIsRecognisedByItsRowId() {
        val old = Task(id = 7, title = "old", parent = rootId, remoteId = null)
        trees.merge(root, rootId, listOf(TaskContainer(task = old, indent = 0)))

        assertTrue(trees.holds(7, null))
    }

    @Test
    fun aSubtaskOnItsWayOutIsNotSomethingToOpen() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1), row(3, "c"))

        trees.delete(key("a"))

        assertTrue(trees.isDoomed(1, "uuid-a"))
        assertTrue(trees.isDoomed(2, "uuid-b"))
        assertFalse(trees.isDoomed(3, "uuid-c"))
        assertFalse(trees.isDoomed(99, "uuid-elsewhere"))
    }

    @Test
    fun aSubtaskPutBackIsSomethingToOpenAgain() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        trees.delete(key("a"))

        trees.restore(key("a"))

        assertFalse(trees.isDoomed(1, "uuid-a"))
        assertFalse(trees.isDoomed(2, "uuid-b"))
    }

    @Test
    fun whatTheTaskListNeedsToKnowAboutDeletions() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c", parent = 2, indent = 2),
            row(4, "d"),
        )

        trees.delete(key("a"))

        assertEquals(
            mapOf(key("a") to true, key("b") to false, key("c") to false),
            trees.nodes.value.deletions(),
        )
    }

    @Test
    fun aListWithNothingDeletedHasNothingToDraw() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))

        assertTrue(trees.nodes.value.deletions().isEmpty())
    }

    @Test
    fun theChevronDoesNotCountWhatIsInsideADeletedSubtask() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c", parent = 2, indent = 2),
        )
        assertEquals(2, trees.rowsOf(root).first().children)

        trees.delete(key("b"))

        assertEquals(1, trees.rowsOf(root).first().children)
    }

    @Test
    fun aRingInTheTreeIsNotWalkedForever() {
        merge(row(1, "a"))
        trees.merge(
            key("a"),
            1,
            listOf(TaskContainer(task = Task(id = rootId, title = "up", parent = 1, remoteId = root))),
        )
        assertEquals(root, trees.get(key("a"))!!.parentKey)
        assertEquals(key("a"), trees.get(root)!!.parentKey)

        assertEquals(listOf("a"), trees.rowsOf(root).map { it.node.title })
        assertEquals(setOf(key("a")), trees.nodes.value.subtreeOf(root).keys)
    }

    @Test
    fun aTickStillCarriesOntoRowsWithNothingOfTheirOwnStaged() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c", parent = 2, indent = 2),
        )

        trees.setCompleted(key("a"), true)

        assertEquals(listOf(true, true, true), completions())
    }

    @Test
    fun aDeletionSwallowsWhatIsNestedInsideItOnAnyList() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c", parent = 2, indent = 2),
        )
        trees.delete(key("a"))

        assertEquals(setOf(key("a"), key("b"), key("c")), trees.rowsOf(root).doomed())
        assertEquals(listOf("a"), trees.rowsOf(root).visible().map { it.node.title })
    }

    @Test
    fun theTaskListSeesWhatTheEditorHasMarkedForDeletion() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        trees.delete(key("a"))

        assertEquals(
            mapOf(key("a") to true, key("b") to false),
            trees.nodes.value.deletions(),
        )

        assertTrue(trees.isDoomed(1, "uuid-a"))
        assertTrue(trees.isDoomed(2, "uuid-b"))
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
