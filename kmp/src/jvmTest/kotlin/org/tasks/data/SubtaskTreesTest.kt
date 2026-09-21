package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Task

class SubtaskTreesTest {
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
    fun tickingASubtaskTicksWhatIsNestedInsideIt() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c", parent = 2, indent = 2),
            row(4, "d"),
        )

        trees.setCompleted(key("a"), true)

        assertEquals(listOf(true, true, true, false), completions())
    }

    @Test
    fun unTickingASubtaskUnTicksWhatIsInsideItAndWhatIsHoldingIt() {
        merge(
            row(1, "a", completed = true),
            row(2, "b", parent = 1, indent = 1, completed = true),
            row(3, "c", parent = 2, indent = 2, completed = true),
            row(4, "d", completed = true),
        )

        trees.setCompleted(key("b"), false)

        assertEquals(listOf(false, false, false, true), completions())
    }

    @Test
    fun aRowTheCascadeReachesThatAlreadyAgreesIsLeftUnstaged() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1, completed = true))

        trees.setCompleted(key("a"), true)

        assertTrue(trees.get(key("b"))!!.completed)
        assertFalse(trees.get(key("b"))!!.completionEdited)
        assertTrue(trees.get(key("a"))!!.completionEdited)
    }

    @Test
    fun tickingASubtaskLeavesTheOnesHoldingItAlone() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))

        trees.setCompleted(key("b"), true)

        assertEquals(listOf(false, true), completions())
    }

    @Test
    fun unTickingASubtaskAndTickingItAgainLeavesNothingStagedAnywhere() {
        merge(
            row(1, "a", completed = true),
            row(2, "b", parent = 1, indent = 1, completed = true),
        )

        trees.setCompleted(key("b"), false)
        assertEquals(listOf(false, false), completions())
        trees.setCompleted(key("b"), true)

        assertEquals(listOf(true, true), completions())
        assertFalse(trees.get(key("a"))!!.completionEdited)
        assertFalse(trees.get(key("b"))!!.completionEdited)
    }

    @Test
    fun aTickTakenBackLeavesNothingStaged() {
        merge(row(1, "a"))
        trees.setCompleted(key("a"), true)

        trees.setCompleted(key("a"), false)

        assertFalse(trees.get(key("a"))!!.completed)
        assertFalse(trees.get(key("a"))!!.completionEdited)
    }

    @Test
    fun restorePutsADeletedSubtaskBack() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        trees.delete(key("a"))

        trees.restore(key("a"))

        assertEquals(listOf("a", "  b"), trees.rowsOf(root).visible().map {
            "${"  ".repeat(it.indent)}${it.node.title}"
        })
        assertTrue(trees.rowsOf(root).doomed().isEmpty())
    }

    @Test
    fun restoringASubtaskLeavesOneDeletedInsideItDeleted() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        trees.delete(key("b"))
        trees.delete(key("a"))

        trees.restore(key("a"))

        assertEquals(listOf("a", "  b"), trees.rowsOf(root).visible().map {
            "${"  ".repeat(it.indent)}${it.node.title}"
        })
        assertEquals(setOf(key("b")), trees.rowsOf(root).doomed())
    }

    @Test
    fun aDropAfterASiblingThatIsNotThereDoesNotSendTheRowToTheFront() {
        merge(row(1, "a"), row(2, "b"), row(3, "c"))

        trees.move(key = key("a"), parentKey = root, after = "uuid-gone")

        assertEquals(listOf("b", "c", "a"), tree())
    }

    @Test
    fun foldingASubtaskThatHasNoRowIsHeldOnTheNode() {
        val added = trees.add(root, Task(title = "new", remoteId = "uuid-new"), list = null)
        trees.add(added.key, Task(title = "inner", remoteId = "uuid-inner"), list = null)

        trees.setCollapsed(added.key, true)

        assertEquals(listOf("new"), trees.rowsOf(root).visible().map { it.node.title })
        assertTrue(trees.get(added.key)!!.task.isCollapsed)
    }

    @Test
    fun dropTakesASubtaskOutOfTheTreeAltogether() {
        merge(row(1, "a"))
        val added = trees.add(root, Task(title = "new", remoteId = "uuid-new"), list = null)

        trees.drop(added.key)

        assertNull(trees.get(added.key))
        assertEquals(listOf("a"), tree())
    }

    @Test
    fun rearrangingInsideASubtaskMarksTheTaskItHangsFrom() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        assertFalse(trees.isRearranged(root))

        trees.setTitle(key("b"), "renamed")

        assertTrue(trees.isRearranged(root))
    }

    @Test
    fun addingASubtaskInsideASubtaskMarksTheTaskItHangsFrom() {
        merge(row(1, "a"))

        trees.add(key("a"), Task(title = "new", remoteId = "uuid-new"), list = null)

        assertTrue(trees.isRearranged(root))
    }

    @Test
    fun aSubtaskCannotBeMovedInsideItself() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))

        trees.move(key = key("a"), parentKey = key("b"), after = null)

        assertEquals(listOf("a", "  b"), tree())
    }

    @Test
    fun clearDropsTheWholeTree() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        trees.setTitle(key("b"), "renamed")

        trees.clear()

        assertEquals(emptyList<String>(), tree())
        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun aRearrangementInsideASubtaskIsAChangeToThatSubtaskToo() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))

        trees.setTitle(key("b"), "renamed")

        assertTrue(trees.isRearranged(key("a")))
        assertTrue(trees.isRearranged(root))
    }

    @Test
    fun aSubtaskAddedInsideASubtaskIsAChangeToThatSubtaskToo() {
        merge(row(1, "a"))

        trees.add(key("a"), Task(title = "new", remoteId = "uuid-new"), list = null)

        assertTrue(trees.isRearranged(key("a")))
    }

    @Test
    fun aTickTakenBackLeavesNothingToSave() {
        merge(row(1, "a"), row(2, "b"))
        trees.setCompleted(key("a"), true)
        assertTrue(trees.isRearranged(root))

        trees.setCompleted(key("a"), false)

        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun aDeletionTakenBackLeavesNothingToSave() {
        merge(row(1, "a"))
        trees.delete(key("a"))
        assertTrue(trees.isRearranged(root))

        trees.restore(key("a"))

        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun revertTakesBackWhatWasStagedAndLeavesTheRowAlone() {
        merge(row(1, "a"), row(2, "b"))
        val renamed = trees.setTitle(key("a"), "renamed")
        val ticked = trees.setCompleted(key("a"), true)
        trees.delete(key("b"))

        trees.revert(ticked + renamed)
        trees.restoreDeletions(mapOf(key("b") to false))

        assertEquals("a", trees.get(key("a"))!!.title)
        assertFalse(trees.get(key("a"))!!.completionEdited)
        assertFalse(trees.get(key("b"))!!.deleted)
        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun revertLeavesUntouchedSubtasksAlone() {
        merge(row(1, "a"), row(2, "b"))
        val staged = trees.setTitle(key("a"), "renamed")
        trees.delete(key("b"))

        trees.revert(staged)

        assertEquals("a", trees.get(key("a"))!!.title)
        assertTrue(trees.get(key("b"))!!.deleted)
    }

    @Test
    fun revertLeavesADeletionStagedSomewhereElseAlone() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        trees.delete(key("b"))
        val touched = trees.setCompleted(key("a"), true)

        trees.revert(touched)

        assertTrue(trees.get(key("b"))!!.deleted)
        assertFalse(trees.get(key("a"))!!.completionEdited)
    }

    @Test
    fun aDeletionIsPutBackToTheMarkItCarriedFirst() {
        merge(row(1, "a"), row(2, "b"))
        trees.delete(key("a"))

        trees.delete(key("b"))
        trees.restoreDeletions(mapOf(key("a") to true, key("b") to false))

        assertTrue(trees.get(key("a"))!!.deleted)
        assertFalse(trees.get(key("b"))!!.deleted)
    }

    @Test
    fun aTreeKeepsWhatWasStagedWhileItWasBeingWritten() {
        merge(row(1, "a"), row(2, "b"))
        val written = trees.rowsOf(root).map { it.node }
        trees.setTitle(key("b"), "renamed")

        trees.clearWritten(written)

        assertNull(trees.get(key("a")))
        assertEquals("renamed", trees.get(key("b"))?.title)
    }

    @Test
    fun aRowAtTheTopOfAStackedEditorHasNowhereFurtherOutToGo() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c", parent = 2, indent = 2),
        )

        assertFalse(trees.outdent(rootKey = key("a"), key = key("b")))

        assertEquals(listOf("a", "  b", "    c"), tree())
        assertFalse(trees.isRearranged(key("a")))
    }

    @Test
    fun aNestedRowInAStackedEditorStillComesOutOneLevel() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1),
            row(3, "c", parent = 2, indent = 2),
        )

        assertTrue(trees.outdent(rootKey = key("a"), key = key("c")))

        assertEquals(listOf("a", "  b", "  c"), tree())
    }

    @Test
    fun aCreatedSubtaskStopsBeingSomethingToCreate() {
        val added = trees.add(root, Task(title = "new", remoteId = "uuid-new"), list = null)
        assertTrue(trees.get(added.key)!!.isNew)

        trees.settle(mapOf(added.key to Task(id = 7, title = "new", remoteId = "uuid-new")))

        val settled = trees.get(added.key)!!
        assertFalse(settled.isNew)
        assertEquals(7L, settled.id)
    }

    @Test
    fun renamingASubtaskAndTypingTheOldNameBackLeavesNothingToSave() {
        merge(row(1, "Milk"))

        trees.setTitle(key("Milk"), "Milkk")
        assertTrue(trees.isRearranged(root))
        trees.setTitle(key("Milk"), "Milk")

        assertNull(trees.get(key("Milk"))!!.stagedTitle)
        assertFalse(trees.get(key("Milk"))!!.titleEdited)
        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun droppingASubtaskLeavesWhatWasDraggedUnderItWhereItCanBeReached() {
        merge(row(1, "a"))
        val added = trees.add(root, Task(title = "new", remoteId = "uuid-new"), list = null)
        trees.move(key = key("a"), parentKey = added.key, after = null)
        trees.setTitle(key("a"), "renamed")
        assertEquals(listOf("new", "  renamed"), tree())

        trees.drop(added.key)

        assertEquals(listOf("renamed"), tree())
        assertEquals("renamed", trees.get(key("a"))!!.title)
    }

    @Test
    fun aSubtaskStagedWhileItsParentWasBeingWrittenIsStillReachable() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        val written = trees.rowsOf(root).map { it.node }
        trees.setTitle(key("b"), "renamed")

        trees.clearWritten(written)

        assertEquals(listOf("a", "  renamed"), tree())
        assertEquals(key("a"), trees.get(key("b"))!!.parentKey)
        assertTrue(trees.isRearranged(root))
    }

    @Test
    fun aParentIsDroppedOnceNothingIsLeftUnderneathIt() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        val written = trees.rowsOf(root).map { it.node }

        trees.clearWritten(written)

        assertTrue(trees.nodes.value.isEmpty())
    }

    @Test
    fun anArrangementCanBePutBackTheWayItWasFound() {
        merge(row(1, "a"), row(2, "b"), row(3, "c"))
        val before = trees.arrangementUnder(root)

        trees.move(key = key("c"), parentKey = root, after = null)
        trees.indent(key("b"))
        assertEquals(listOf("c", "a", "  b"), tree())

        trees.restoreArrangement(root, before)

        assertEquals(listOf("a", "b", "c"), tree())
        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun anArrangementIsNotPutBackSomewhereNothingCanReach() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        val before = trees.arrangementUnder(root)
        trees.move(key = key("b"), parentKey = root, after = null)
        trees.clearWritten(listOf(trees.get(key("a"))!!))

        trees.restoreArrangement(root, before)

        assertEquals(listOf("b"), tree())
        assertEquals(root, trees.get(key("b"))!!.parentKey)
    }

    @Test
    fun aTickTakenBackLeavesASubtaskThatWasAlreadyDoneExactlyAsItWas() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1, completed = true))
        assertEquals(listOf(false, true), completions())

        trees.setCompleted(key("a"), true)
        assertEquals(listOf(true, true), completions())
        trees.setCompleted(key("a"), false)

        assertEquals(listOf(false, true), completions())
        assertFalse(trees.get(key("a"))!!.completionEdited)
        assertFalse(trees.get(key("b"))!!.completionEdited)
        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun aTickDoesNotSwallowAnUnTickMadeFurtherDown() {
        merge(
            row(1, "a"),
            row(2, "b", parent = 1, indent = 1, completed = true),
        )

        trees.setCompleted(key("b"), false)
        trees.setCompleted(key("a"), true)
        trees.setCompleted(key("a"), false)

        assertFalse(trees.get(key("b"))!!.completed)
        assertTrue(trees.get(key("b"))!!.completionEdited)
        assertFalse(trees.get(key("a"))!!.completionEdited)
    }

    @Test
    fun setCompletedNamesOnlyTheRowsItActuallyChanged() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1, completed = true))

        val touched = trees.setCompleted(key("a"), true)

        assertEquals(setOf(key("a")), touched.keys)
    }

    @Test
    fun setCompletedNamesTheRowsItStagedOn() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1), row(3, "c"))

        val touched = trees.setCompleted(key("a"), true)

        assertEquals(setOf(key("a")), touched.keys)
    }

    @Test
    fun aTitleTypedWhileTheRowWasBeingCreatedSurvivesTheSettle() {
        val added = trees.add(root, Task(title = "", remoteId = "uuid-new"), list = null)
        trees.setTitle(added.key, "milk")
        val written = trees.get(added.key)!!
        trees.takePending(added.key)
        trees.setTitle(added.key, "milk 2%")

        trees.settle(mapOf(added.key to Task(id = 7, title = "milk", remoteId = "uuid-new")))

        val settled = trees.get(added.key)!!
        assertNull(settled.pending)
        assertEquals("milk 2%", settled.title)
        assertTrue(settled.titleEdited)
        assertFalse(clearWrittenLeaves(listOf(written.copy(pending = null))))
    }

    @Test
    fun aTitleClearedWhileTheRowWasBeingCreatedSurvivesTheSettle() {
        val added = trees.add(root, Task(title = "draft", remoteId = "uuid-new"), list = null)
        val written = trees.get(added.key)!!
        trees.setTitle(added.key, "")

        trees.settle(
            created = mapOf(added.key to Task(id = 7, title = "draft", remoteId = "uuid-new")),
            applied = mapOf(added.key to written),
        )

        val settled = trees.get(added.key)!!
        assertTrue(settled.titleEdited)
        assertEquals("", settled.stagedTitle)
    }

    @Test
    fun aBlankTheRowWasCreatedFromIsStillCleared() {
        val added = trees.add(root, Task(title = "draft", remoteId = "uuid-new"), list = null)
        trees.setTitle(added.key, "")
        val written = trees.get(added.key)!!

        trees.settle(
            created = mapOf(added.key to Task(id = 7, title = "(no title)", remoteId = "uuid-new")),
            applied = mapOf(added.key to written),
        )

        assertFalse(trees.get(added.key)!!.titleEdited)
    }

    @Test
    fun aTitleThatWasWrittenWithTheRowIsCleared() {
        val added = trees.add(root, Task(title = "", remoteId = "uuid-new"), list = null)
        trees.setTitle(added.key, "milk")

        trees.takePending(added.key)
        trees.settle(mapOf(added.key to Task(id = 7, title = "milk", remoteId = "uuid-new")))

        val settled = trees.get(added.key)!!
        assertFalse(settled.titleEdited)
        assertFalse(settled.needsWriting)
    }

    @Test
    fun aTickThatTheWriteNeverReachedIsKeptForTheNextSave() {
        val added = trees.add(root, Task(title = "buy", remoteId = "uuid-new"), list = null)
        trees.setCompleted(added.key, true)

        trees.settle(mapOf(added.key to Task(id = 7, title = "buy", remoteId = "uuid-new")))

        assertTrue(trees.get(added.key)!!.completionEdited)
        assertTrue(trees.isRearranged(root))
    }

    @Test
    fun aTickTheWriteCarriedOutIsCleared() {
        val added = trees.add(root, Task(title = "buy", remoteId = "uuid-new"), list = null)
        trees.setCompleted(added.key, true)
        val applied = trees.get(added.key)!!

        trees.settle(
            created = mapOf(added.key to Task(id = 7, title = "buy", remoteId = "uuid-new")),
            applied = mapOf(added.key to applied),
        )

        assertFalse(trees.get(added.key)!!.completionEdited)
    }

    @Test
    fun anArrangementThatInvertedAParentAndItsChildIsPutBackInFull() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        val before = trees.arrangementUnder(root)

        trees.outdent(root, key("b"))
        trees.move(key = key("a"), parentKey = root, after = key("b"))
        trees.indent(key("a"))
        assertEquals(listOf("b", "  a"), tree())

        trees.restoreArrangement(root, before)

        assertEquals(listOf("a", "  b"), tree())
        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun anUnTickTakesBackItsOwnTickAndNothingElse() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))

        trees.setCompleted(key("b"), true)
        trees.setCompleted(key("a"), true)
        trees.setCompleted(key("a"), false)

        assertTrue(trees.get(key("b"))!!.completed)
        assertTrue(trees.get(key("b"))!!.completionEdited)
        assertFalse(trees.get(key("a"))!!.completionEdited)
        assertEquals(listOf(false, true), completions())
    }

    @Test
    fun aCascadedUnTickCanBeTickedBackOn() {
        merge(
            row(1, "a", completed = true),
            row(2, "b", parent = 1, indent = 1, completed = true),
        )

        trees.setCompleted(key("a"), false)
        assertEquals(listOf(false, false), completions())
        assertFalse(trees.get(key("b"))!!.completionEdited)

        trees.setCompleted(key("a"), true)

        assertEquals(listOf(true, true), completions())
        assertFalse(trees.get(key("a"))!!.completionEdited)
        assertFalse(trees.get(key("b"))!!.completionEdited)
        assertFalse(trees.isRearranged(root))
    }

    @Test
    fun revertPutsBackWhatTheOtherEditorHadStagedOnTheSameRow() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        trees.setTitle(key("b"), "renamed above")

        val displaced = trees.setCompleted(key("b"), true)
        trees.revert(displaced)

        assertEquals("renamed above", trees.get(key("b"))!!.title)
        assertTrue(trees.get(key("b"))!!.titleEdited)
        assertFalse(trees.get(key("b"))!!.completionEdited)
        assertTrue(trees.isRearranged(root))
    }

    @Test
    fun revertPutsBackACascadedTickRatherThanClearingIt() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        trees.setCompleted(key("a"), true)

        val displaced = trees.setCompleted(key("b"), false)
        trees.revert(displaced)

        assertEquals(listOf(true, true), completions())
        assertFalse(trees.get(key("b"))!!.completionEdited)
    }

    @Test
    fun droppingARowSlotsWhatWasInsideItIntoItsPlace() {
        merge(row(1, "a"), row(2, "b"), row(3, "c"))
        val added = trees.addAfter(trees.get(key("a"))!!, Task(remoteId = "uuid-new"), list = null)
        trees.indent(key("b"))
        assertEquals(listOf("a", "null", "  b", "c"), tree())

        trees.drop(added.key)

        assertEquals(listOf("a", "b", "c"), tree())
    }

    @Test
    fun contentStagedWhileTheRowWasBeingCreatedIsNotSettledAway() {
        val added = trees.add(root, Task(title = "buy", remoteId = "uuid-new"), list = null)
        assertNotNull(trees.takePending(added.key))
        trees.update(added.key) { it.copy(pending = PendingTask(list = null, tags = emptyList())) }

        trees.settle(mapOf(added.key to Task(id = 7, title = "buy", remoteId = "uuid-new")))

        val settled = trees.get(added.key)!!
        assertNotNull(settled.pending)
        assertFalse(settled.isNew)
        assertTrue(settled.pendingUnwritten)
        assertTrue(settled.needsWriting)
    }

    @Test
    fun contentStagedWhileTheRowWasBeingCreatedIsNotDroppedAsWritten() {
        val added = trees.add(root, Task(title = "buy", remoteId = "uuid-new"), list = null)
        trees.takePending(added.key)
        val settled = trees.settle(
            mapOf(added.key to Task(id = 7, title = "buy", remoteId = "uuid-new"))
        )
        trees.update(added.key) { it.copy(pending = PendingTask(list = null)) }

        assertFalse(clearWrittenLeaves(settled.values.toList()))
    }

    @Test
    fun closingAnEditorLeavesNothingBehind() {
        merge(row(1, "a"), row(2, "b", parent = 1, indent = 1))
        trees.move(key = key("b"), parentKey = root, after = null)
        trees.drop(key("a"))

        trees.clear()

        assertTrue(trees.nodes.value.isEmpty())
    }

    @Test
    fun aRowThatChangedUnderneathTheWriteIsStillFinishedWith() {
        merge(row(1, "a"))
        trees.setTitle(key("a"), "renamed")
        val written = trees.get(key("a"))!!
        merge(row(1, "a", completed = true))

        assertTrue(clearWrittenLeaves(listOf(written)))
    }

    @Test
    fun aRowStagedOnAgainAfterTheWriteIsNotFinishedWith() {
        merge(row(1, "a"))
        trees.setTitle(key("a"), "renamed")
        val written = trees.get(key("a"))!!
        trees.setTitle(key("a"), "renamed twice")

        assertFalse(clearWrittenLeaves(listOf(written)))
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
