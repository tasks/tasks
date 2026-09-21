package org.tasks.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Task

class SubtaskTreeRegistryTest {
    private val registry = SubtaskTreeRegistry()

    private fun SubtaskTrees.subtask(id: Long, title: String, root: String = "root"): SubtaskNode {
        merge(root, ROOT_ID, listOf(row(id, title)))
        return get("uuid-$title")!!
    }

    private fun row(id: Long, title: String) = TaskContainer(
        task = Task(id = id, title = title, parent = ROOT_ID, remoteId = "uuid-$title"),
    )

    private suspend fun deletions() = registry.deletions.first()

    @Test
    fun eachEditorIsGivenATreeOfItsOwn() {
        val one = registry.open()
        val two = registry.open()

        assertFalse(one === two)
        assertEquals(listOf(one, two), registry.openTrees)
    }

    @Test
    fun aTaskIsRecognisedWhicheverOpenEditorIsHoldingIt() {
        registry.open()
        val second = registry.open()
        second.subtask(1, "a")

        assertTrue(registry.holds(1, "uuid-a"))
        assertFalse(registry.holds(2, "uuid-elsewhere"))
    }

    @Test
    fun aTaskStopsBeingRecognisedWhenTheEditorHoldingItCloses() {
        val tree = registry.open()
        tree.subtask(1, "a")

        registry.close(tree)

        assertFalse(registry.holds(1, "uuid-a"))
        assertEquals(emptyList<SubtaskTrees>(), registry.openTrees)
    }

    @Test
    fun aRowOnItsWayOutIsRecognisedWhicheverEditorMarkedIt() {
        registry.open()
        val second = registry.open()
        val node = second.subtask(1, "a")
        second.delete(node.key)

        assertTrue(registry.isDoomed(1, "uuid-a"))
        assertFalse(registry.isDoomed(2, "uuid-elsewhere"))
    }

    @Test
    fun theTaskListSeesDeletionsFromEveryOpenEditorAtOnce() = runBlocking {
        val one = registry.open()
        val two = registry.open()
        one.delete(one.subtask(1, "a").key)
        two.delete(two.subtask(2, "b").key)

        assertEquals(
            mapOf("uuid-a" to true, "uuid-b" to true),
            deletions(),
        )
    }

    @Test
    fun aDeletionGoesWhenTheEditorHoldingItCloses() = runBlocking {
        val tree = registry.open()
        tree.delete(tree.subtask(1, "a").key)
        assertEquals(mapOf("uuid-a" to true), deletions())

        registry.close(tree)

        assertEquals(emptyMap<String, Boolean>(), deletions())
    }

    @Test
    fun nothingIsDeletedWhileNoEditorIsOpen() = runBlocking {
        assertEquals(emptyMap<String, Boolean>(), deletions())
    }

    @Test
    fun theTreeHoldingAKeyIsTheOneAnEditorOnThatSubtaskStagesInto() {
        registry.open()
        val second = registry.open()
        val node = second.subtask(1, "a")

        assertSame(second, registry.holding(node.key))
    }

    @Test
    fun noTreeIsHoldingAKeyNobodyHasStaged() {
        registry.open()

        assertNull(registry.holding("uuid-gone"))
    }

    companion object {
        private const val ROOT_ID = 42L
    }
}
