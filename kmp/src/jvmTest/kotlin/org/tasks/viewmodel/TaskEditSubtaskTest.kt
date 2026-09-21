package org.tasks.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.check
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.data.SubtaskNode
import org.tasks.data.SubtaskTreeWriter
import java.lang.reflect.Modifier
import org.tasks.data.PendingTask
import org.tasks.data.TaskContainer
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.compose.pickers.DAY_BEFORE_DUE
import org.tasks.compose.pickers.NO_TIME
import org.tasks.filters.CaldavFilter
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_HOUR
import org.tasks.time.minusDays
import org.tasks.time.plusDays
import org.tasks.time.startOfDay

@OptIn(ExperimentalCoroutinesApi::class)
class TaskEditSubtaskTest : TaskEditViewModelFixture() {
    private val STAGED = setOf(
        "addSubtask",
        "setSubtaskTitle",
        "toggleSubtaskComplete",
        "removeSubtask",
        "backspaceSubtask",
        "restoreSubtask",
        "moveSubtask",
        "indentSubtask",
    )

    private val UNSTAGED = setOf(
        "onSubtaskFocused",
        "toggleSubtaskCollapsed",
    )

    private val googleAccount =
        CaldavAccount(uuid = "google-1", accountType = CaldavAccount.TYPE_GOOGLE_TASKS)
    private val googleCalendar = CaldavCalendar(account = "google-1", uuid = "google-cal")
    private val googleList = CaldavFilter(calendar = googleCalendar, account = googleAccount)

    @Test
    fun backspacingANewSubtaskAwayThrowsItAwayAltogether() = runTest(testDispatcher) {
        buildViewModel(remoteId = "parent-uuid")
        advanceUntilIdle()
        val subtask = addSubtask("Subtask")

        viewModel.backspaceSubtask(subtask)
        advanceUntilIdle()

        assertTrue(tree().isEmpty())
        assertNull(subtaskTrees.get(subtask.key))
    }

    @Test
    fun backspacingAnExistingSubtaskAwayGivesItItsTitleBack() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        viewModel.setSubtaskTitle(node("a"), "")
        advanceUntilIdle()

        viewModel.backspaceSubtask(viewModel.state.value.subtasks.first().node)
        advanceUntilIdle()

        assertEquals(listOf("a"), tree())
        assertTrue(deleted("a"))

        viewModel.save()
        advanceUntilIdle()

        verify(taskDeleter).markDeleted(listOf(1L))
    }

    @Test
    fun aSubtaskBackspacedAwayAndPutBackKeepsItsOwnTitle() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        viewModel.setSubtaskTitle(node("a"), "")
        advanceUntilIdle()
        viewModel.backspaceSubtask(viewModel.state.value.subtasks.first().node)
        advanceUntilIdle()

        viewModel.restoreSubtask(node("a"))
        advanceUntilIdle()

        assertEquals(listOf("a"), tree())
        assertFalse(deleted("a"))

        viewModel.save()
        advanceUntilIdle()

        verify(taskDeleter, never()).markDeleted(any<List<Long>>())
    }

    private fun saved(id: Long, title: String, parent: Long = 42, indent: Int = 0) =
        TaskContainer(
            task = Task(id = id, title = title, parent = parent, remoteId = "uuid-$title"),
            indent = indent,
        )

    private fun TaskContainer.collapsed(): TaskContainer =
        copy(task = task.copy(isCollapsed = true))

    private suspend fun TestScope.initializeWithSubtasks(vararg rows: TaskContainer) {
        whenever(taskDao.fetchTasks(any<String>())).thenReturn(rows.toList())
        rows.forEach { createdRows[it.id] = it.task }
        rows.forEach { whenever(taskDao.fetch(it.id)).thenReturn(it.task) }
        initializeExisting()
    }

    private fun tree(vm: TaskEditViewModel = viewModel): List<String> =
        vm.state.value.subtasks.map { "${"  ".repeat(it.indent)}${it.node.title}" }

    private fun deleted(title: String, vm: TaskEditViewModel = viewModel): Boolean =
        node(title, vm).deleted

    private fun node(title: String, vm: TaskEditViewModel = viewModel): SubtaskNode =
        vm.state.value.subtasks.first { it.node.title == title }.node

    private fun TestScope.addSubtask(title: String?, after: SubtaskNode? = null): SubtaskNode {
        viewModel.addSubtask(after = after)
        advanceUntilIdle()
        val key = viewModel.state.value.focusSubtask!!
        if (title != null) {
            viewModel.setSubtaskTitle(subtaskTrees.get(key)!!, title)
            advanceUntilIdle()
        }
        return subtaskTrees.get(key)!!
    }

    @Test
    fun addedSubtaskIsNotWrittenUntilTheTaskIsSaved() = runTest(testDispatcher) {
        initializeNew()

        addSubtask("Subtask")

        assertEquals(listOf("Subtask"), tree())
        verify(taskDao, never()).createNew(any())
        assertTrue(viewModel.state.value.hasChanges)
    }

    @Test
    fun anAddedSubtaskCanBeDraggedAboveOneThatAlreadyExists() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "old"))
        val new = addSubtask("new")
        assertEquals(listOf("old", "new"), tree())

        viewModel.moveSubtask(fromKey = new.key, toKey = node("old").key)
        advanceUntilIdle()

        assertEquals(listOf("new", "old"), tree())
        verify(taskDao, never()).createNew(any())
    }

    @Test
    fun rearrangingSubtasksThatExistWritesNothingUntilTheTaskIsSaved() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b"))

        viewModel.moveSubtask(fromKey = node("b").key, toKey = node("a").key)
        advanceUntilIdle()

        assertEquals(listOf("b", "a"), tree())
        verify(taskDao, never()).setOrder(any(), anyOrNull())
        verify(taskDao, never()).setParent(any(), any())
        assertTrue(viewModel.state.value.hasChanges)
    }

    @Test
    fun savingWritesTheRearrangement() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b"))
        viewModel.moveSubtask(fromKey = node("b").key, toKey = node("a").key)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        verify(taskDao).setOrder(2L, 0L)
        verify(taskDao).setOrder(1L, 1L)
    }

    @Test
    fun discardingTheTaskLeavesTheSubtasksWhereTheyWere() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b"))
        viewModel.moveSubtask(fromKey = node("b").key, toKey = node("a").key)
        advanceUntilIdle()

        viewModel.discardChanges()
        advanceUntilIdle()

        verify(taskDao, never()).setOrder(any(), anyOrNull())
        assertEquals(listOf("a", "b"), tree())
        assertFalse(viewModel.state.value.subtasksChanged)

        viewModel.onCleared()
        advanceUntilIdle()

        assertTrue(subtaskTrees.isEmpty())
    }

    @Test
    fun nestingASubtaskUnderAnotherIsWrittenAsAReparent() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b"))
        whenever(caldavDao.getTask(any())).thenReturn(CaldavTask(id = 9, task = 1, calendar = "cal-1"))
        whenever(caldavDao.getRemoteIdForTask(any())).thenReturn("remote-a")

        viewModel.indentSubtask(node("b"), steps = 1)
        advanceUntilIdle()
        assertEquals(listOf("a", "  b"), tree())

        viewModel.save()
        advanceUntilIdle()

        verify(taskDao).setParent(1L, listOf(2L))
    }

    @Test
    fun tickingASubtaskIsStagedAndAppliedOnSave() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))

        viewModel.toggleSubtaskComplete(node("a"))
        advanceUntilIdle()

        verify(taskCompleter, never()).setComplete(any<Task>(), any(), any())
        assertTrue(viewModel.state.value.subtasks.single().node.completed)

        viewModel.save()
        advanceUntilIdle()

        verify(taskCompleter).setComplete(check<Task> { assertEquals(1L, it.id) }, eq(true), any())
    }

    @Test
    fun tickingASubtaskTicksWhatIsNestedInsideItStraightAway() = runTest(testDispatcher) {
        initializeWithSubtasks(
            saved(1, "a"),
            saved(2, "b", parent = 1, indent = 1),
            saved(3, "c"),
        )

        viewModel.toggleSubtaskComplete(node("a"))

        assertEquals(
            listOf(true, true, false),
            viewModel.state.value.subtasks.map { it.completed },
        )
    }

    @Test
    fun deletingASubtaskIsStagedUntilTheTaskIsSaved() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))

        viewModel.removeSubtask(node("a"))
        advanceUntilIdle()

        assertEquals(listOf("a"), tree())
        assertTrue(deleted("a"))
        verify(taskDeleter, never()).markDeleted(any<List<Long>>())
        assertTrue(viewModel.state.value.hasChanges)

        viewModel.save()
        advanceUntilIdle()

        verify(taskDeleter).markDeleted(listOf(1L))
    }

    @Test
    fun restoringASubtaskCallsOffTheDeletion() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        viewModel.removeSubtask(node("a"))
        advanceUntilIdle()

        viewModel.restoreSubtask(node("a"))
        advanceUntilIdle()

        assertFalse(deleted("a"))

        viewModel.save()
        advanceUntilIdle()

        verify(taskDeleter, never()).markDeleted(any<List<Long>>())
    }

    @Test
    fun whatIsInsideADeletedSubtaskIsNotDrawnWithIt() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b", parent = 1, indent = 1), saved(3, "c"))

        viewModel.removeSubtask(node("a"))
        advanceUntilIdle()

        assertEquals(listOf("a", "c"), tree())
    }

    @Test
    fun discardingTheTaskPutsBackASubtaskDeletedOnIt() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        viewModel.removeSubtask(node("a"))
        advanceUntilIdle()

        viewModel.discardChanges()
        advanceUntilIdle()

        verify(taskDeleter, never()).markDeleted(any<List<Long>>())
    }

    @Test
    fun aSubtaskThatAppearsWhileEditingJoinsTheTreeWithoutDisturbingIt() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b"))
        viewModel.moveSubtask(fromKey = node("b").key, toKey = node("a").key)
        advanceUntilIdle()
        assertEquals(listOf("b", "a"), tree())

        whenever(taskDao.fetchTasks(any<String>()))
            .thenReturn(listOf(saved(1, "a"), saved(2, "b"), saved(3, "c")))
        refreshes.emit(Unit)
        advanceUntilIdle()

        assertEquals(listOf("b", "a", "c"), tree())
    }

    @Test
    fun savingCreatesAddedSubtasksUnderTheTask() = runTest(testDispatcher) {
        initializeNew()
        whenever(caldavDao.getRemoteIdForTask(any())).thenReturn("parent-remote-id")

        viewModel.setTitle("Parent")
        addSubtask("Subtask")
        viewModel.save()
        advanceUntilIdle()

        val parentId = viewModel.state.value.task.id
        verify(taskDao).createNew(check { assertEquals("Parent", it.title) })
        verify(taskDao).createNew(
            check {
                assertEquals("Subtask", it.title)
                assertEquals(parentId, it.parent)
            }
        )
        verify(caldavDao).insert(
            task = check { assertEquals("Subtask", it.title) },
            caldavTask = check { assertEquals("parent-remote-id", it.remoteParent) },
            addToTop = eq(false),
        )
    }

    @Test
    fun blankSubtaskIsNotCreated() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setTitle("Parent")
        addSubtask(title = null)
        viewModel.save()
        advanceUntilIdle()

        verify(taskDao, times(1)).createNew(any())
    }

    @Test
    fun nestedSubtasksAreCreatedUnderTheirOwnParent() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Parent")
        addSubtask("Subtask")
        val nested = addSubtask("Nested")
        viewModel.indentSubtask(nested, steps = 1)
        advanceUntilIdle()
        assertEquals(listOf("Subtask", "  Nested"), tree())

        viewModel.save()
        advanceUntilIdle()

        val subtaskId = createdRows.values.first { it.title == "Subtask" }.id
        assertEquals(subtaskId, createdRows.values.first { it.title == "Nested" }.parent)
    }

    @Test
    fun nestedSubtasksAreCreatedFlatOnAListThatHoldsOneLevel() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Parent")
        addSubtask("Subtask")
        val nested = addSubtask("Nested")
        viewModel.indentSubtask(nested, steps = 1)
        advanceUntilIdle()
        viewModel.setList(googleList)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        val parentId = viewModel.state.value.task.id
        assertEquals(parentId, createdRows.values.first { it.title == "Subtask" }.parent)
        assertEquals(parentId, createdRows.values.first { it.title == "Nested" }.parent)
    }

    @Test
    fun subtasksCanStillBeNestedOnTheWayToAListThatHoldsOneLevel() = runTest(testDispatcher) {
        initializeNew()
        addSubtask("first")
        val second = addSubtask("second")
        viewModel.setList(googleList)
        advanceUntilIdle()

        viewModel.indentSubtask(second, steps = 1)
        advanceUntilIdle()

        assertEquals(listOf("first", "  second"), tree())
        assertTrue(viewModel.state.value.allowsNesting)
    }

    @Test
    fun subtasksCannotBeNestedOnAListThatHoldsOneLevelAndIsStayingThere() =
        runTest(testDispatcher) {
            whenever(caldavDao.getCalendars()).thenReturn(listOf(googleCalendar))
            whenever(caldavDao.getAccountByUuid("google-1")).thenReturn(googleAccount)
            whenever(caldavDao.getTask(42)).thenReturn(
                CaldavTask(id = 1, task = 42, calendar = "google-cal")
            )
            initializeWithSubtasks(saved(1, "first"), saved(2, "second"))
            assertFalse(viewModel.state.value.allowsNesting)

            viewModel.indentSubtask(node("second"), steps = 1)
            advanceUntilIdle()

            assertEquals(listOf("first", "second"), tree())
        }

    @Test
    fun nestingCarriedOntoASingleLevelListCanStillBeTakenOutByHand() = runTest(testDispatcher) {
        whenever(caldavDao.getCalendars()).thenReturn(listOf(googleCalendar))
        whenever(caldavDao.getAccountByUuid("google-1")).thenReturn(googleAccount)
        whenever(caldavDao.getTask(42)).thenReturn(
            CaldavTask(id = 1, task = 42, calendar = "google-cal")
        )
        initializeWithSubtasks(saved(1, "first"), saved(2, "second", parent = 1, indent = 1))
        assertEquals(listOf("first", "  second"), tree())

        viewModel.indentSubtask(node("second"), steps = -1)
        advanceUntilIdle()

        assertEquals(listOf("first", "second"), tree())
    }

    @Test
    fun addingAfterASubtaskPutsItDirectlyBelowThatOne() = runTest(testDispatcher) {
        initializeNew()
        val first = addSubtask("first")
        addSubtask("last")

        addSubtask("second", after = first)

        assertEquals(listOf("first", "second", "last"), tree())
    }

    @Test
    fun addingAfterASubtaskAsksForItsFocus() = runTest(testDispatcher) {
        initializeNew()
        val first = addSubtask("first")

        val second = addSubtask(title = null, after = first)

        assertEquals(second.key, viewModel.state.value.focusSubtask)
        viewModel.onSubtaskFocused(second.key)
        assertNull(viewModel.state.value.focusSubtask)
    }

    @Test
    fun tabNestsASubtaskUnderTheOneAboveIt() = runTest(testDispatcher) {
        initializeNew()
        addSubtask("first")
        val second = addSubtask("second")

        viewModel.indentSubtask(second, steps = 1)
        advanceUntilIdle()

        assertEquals(listOf("first", "  second"), tree())
    }

    @Test
    fun shiftTabMovesASubtaskBackOut() = runTest(testDispatcher) {
        initializeNew()
        addSubtask("first")
        val second = addSubtask("second")
        viewModel.indentSubtask(second, steps = 1)
        advanceUntilIdle()

        viewModel.indentSubtask(subtaskTrees.get(second.key)!!, steps = -1)
        advanceUntilIdle()

        assertEquals(listOf("first", "second"), tree())
    }

    @Test
    fun draggingASubtaskFurtherRightThanTheTreeAllowsStopsWhereItRunsOut() =
        runTest(testDispatcher) {
            initializeNew()
            addSubtask("first")
            val second = addSubtask("second")

            viewModel.indentSubtask(second, steps = 5)
            advanceUntilIdle()

            assertEquals(listOf("first", "  second"), tree())
        }

    @Test
    fun draggingANestedSubtaskAboveItsParentPullsItOutToTheTop() = runTest(testDispatcher) {
        initializeNew()
        val a = addSubtask("A")
        val b = addSubtask("B")
        viewModel.indentSubtask(b, steps = 1)
        advanceUntilIdle()
        assertEquals(listOf("A", "  B"), tree())

        viewModel.moveSubtask(fromKey = b.key, toKey = a.key)
        advanceUntilIdle()

        assertEquals(listOf("B", "A"), tree())
    }

    @Test
    fun draggingASubtaskTakesWhatIsNestedInsideItAlong() = runTest(testDispatcher) {
        initializeNew()
        val a = addSubtask("A")
        val b = addSubtask("B")
        viewModel.indentSubtask(b, steps = 1)
        advanceUntilIdle()
        val c = addSubtask("C")
        assertEquals(listOf("A", "  B", "C"), tree())

        viewModel.moveSubtask(fromKey = a.key, toKey = c.key)
        advanceUntilIdle()

        assertEquals(listOf("C", "A", "  B"), tree())
    }

    @Test
    fun aSubtaskCannotBeDraggedIntoItself() = runTest(testDispatcher) {
        initializeNew()
        val a = addSubtask("A")
        val b = addSubtask("B")
        viewModel.indentSubtask(b, steps = 1)
        advanceUntilIdle()

        viewModel.moveSubtask(fromKey = a.key, toKey = b.key)
        advanceUntilIdle()

        assertEquals(listOf("A", "  B"), tree())
    }

    @Test
    fun editingAnAddedSubtaskWritesToTheTreeAndNotTheDatabase() = runTest(testDispatcher) {
        buildViewModel(remoteId = "parent-uuid")
        advanceUntilIdle()
        val subtask = addSubtask("Subtask")

        buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isDraft)
        assertEquals("Subtask", viewModel.state.value.task.title)

        viewModel.setTitle("Renamed")
        viewModel.setPriority(Task.Priority.HIGH)
        viewModel.save()
        advanceUntilIdle()

        val updated = subtaskTrees.get(subtask.key)!!
        assertEquals("Renamed", updated.task.title)
        assertEquals(Task.Priority.HIGH, updated.task.priority)
        verify(taskDao, never()).createNew(any())
    }

    @Test
    fun aDraftSubtaskKeepsAStartSelectionThatResolvedToNothing() = runTest(testDispatcher) {
        buildViewModel(remoteId = "parent-uuid")
        advanceUntilIdle()
        val subtask = addSubtask("Subtask")

        buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isDraft)
        viewModel.setStartDate(DAY_BEFORE_DUE, NINE_AM_WITH_TIME)
        advanceUntilIdle()
        assertEquals(0L, viewModel.state.value.task.hideUntil)
        viewModel.onCleared()
        advanceUntilIdle()

        buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()

        assertEquals(DAY_BEFORE_DUE, viewModel.state.value.startDay)
        assertEquals(NINE_AM_WITH_TIME, viewModel.state.value.startTime)
        assertFalse(viewModel.state.value.hasChanges)
    }

    @Test
    fun aReopenedDraftSubtaskResolvesItsStartAgainstADueDateGivenLater() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Parent")
        val parent = viewModel
        val subtask = addSubtask("Subtask")

        buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()
        viewModel.setStartDate(DAY_BEFORE_DUE, NO_TIME)
        advanceUntilIdle()
        viewModel.onCleared()
        advanceUntilIdle()

        buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()
        val due = currentTimeMillis().startOfDay().plusDays(3)
        viewModel.setDueDate(due)
        advanceUntilIdle()

        assertEquals(due.minusDays(1), viewModel.state.value.task.hideUntil)

        viewModel.onCleared()
        parent.save()
        advanceUntilIdle()

        assertEquals(
            due.minusDays(1),
            createdRows.values.first { it.title == "Subtask" }.hideUntil,
        )
    }

    @Test
    fun aDraftSubtaskKeepsTheListItWasGivenWhenItsEditorIsReopened() = runTest(testDispatcher) {
        buildViewModel(remoteId = "parent-uuid")
        advanceUntilIdle()
        val subtask = addSubtask("Subtask")
        val other = CaldavFilter(calendar = seedCalendar, account = testAccount)

        buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()
        viewModel.setList(other)
        advanceUntilIdle()
        viewModel.onCleared()
        advanceUntilIdle()

        buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()

        assertEquals(other, viewModel.state.value.list)
        assertFalse(viewModel.state.value.hasChanges)
    }

    @Test
    fun aSubtaskAddedInsideAnotherIsShownIndentedUnderIt() = runTest(testDispatcher) {
        buildViewModel(remoteId = "parent-uuid")
        advanceUntilIdle()
        val parent = viewModel
        val subtask = addSubtask("Subtask")

        buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()
        addSubtask("Nested")

        assertEquals(listOf("Subtask", "  Nested"), tree(parent))
    }

    @Test
    fun deletingASubtaskFromItsOwnEditorStagesItOnTheTaskAboveIt() = runTest(testDispatcher) {
        buildViewModel(remoteId = "parent-uuid")
        advanceUntilIdle()
        val parent = viewModel
        val subtask = addSubtask("Subtask")

        buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()
        viewModel.delete()
        advanceUntilIdle()
        viewModel.onCleared()
        advanceUntilIdle()

        assertEquals(listOf("Subtask"), tree(parent))
        assertTrue(deleted("Subtask", parent))
        verify(taskDeleter, never()).markDeleted(any<List<Long>>())
        verify(taskDao, never()).createNew(any())
    }

    @Test
    fun discardingTheTaskDiscardsTheSubtasksAddedWithIt() = runTest(testDispatcher) {
        buildViewModel(remoteId = "parent-uuid")
        advanceUntilIdle()
        val subtask = addSubtask("Subtask")

        viewModel.discardChanges()
        advanceUntilIdle()

        assertNull(subtaskTrees.get(subtask.key))
        assertTrue(tree().isEmpty())
    }

    @Test
    fun deletingTheTaskDiscardsTheSubtasksAddedWithIt() = runTest(testDispatcher) {
        initializeExisting()
        val subtask = addSubtask("Subtask")

        viewModel.delete()
        advanceUntilIdle()
        viewModel.onCleared()
        advanceUntilIdle()

        assertNull(subtaskTrees.get(subtask.key))
        verify(taskDao, never()).createNew(any())
    }

    @Test
    fun addingASubtaskDoesNotBrieflyShowWhatIsFoldedAwayInsideAnother() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a").collapsed(), saved(2, "b", parent = 1, indent = 1))
        assertEquals(listOf("a"), tree())

        viewModel.addSubtask()

        assertEquals(listOf("a", null), viewModel.state.value.subtasks.map { it.node.title })
    }

    @Test
    fun anEditorOnASubtaskWithNoRowLeavesWhatIsNestedUnderItAlone() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val parent = viewModel
        val added = addSubtask("new")
        viewModel.moveSubtask(fromKey = node("a").key, toKey = added.key, indent = 1)
        advanceUntilIdle()
        assertEquals(listOf("new", "  a"), tree(parent))

        buildViewModel(remoteId = added.key)
        advanceUntilIdle()
        refreshes.emit(Unit)
        advanceUntilIdle()

        assertEquals(listOf("new", "  a"), tree(parent))
        assertEquals(listOf("a"), tree())
    }

    @Test
    fun closingAnEditorThatChangedNothingTakesItsTreeWithIt() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        assertTrue(subtaskTrees.holds(1, "uuid-a"))

        viewModel.onCleared()
        advanceUntilIdle()

        assertTrue(subtaskTrees.isEmpty())
        assertFalse(subtaskTrees.holds(1, "uuid-a"))
    }

    @Test
    fun closingASubtasksOwnEditorLeavesTheTreeToTheTaskAboveIt() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val parent = viewModel
        whenever(taskDao.fetchTasks(any<String>())).thenReturn(emptyList())

        buildViewModel(taskId = 1)
        advanceUntilIdle()
        viewModel.onCleared()
        advanceUntilIdle()

        assertEquals(listOf("a"), tree(parent))
        assertTrue(subtaskTrees.holds(1, "uuid-a"))
    }

    @Test
    fun theSubtaskSectionIsStillFilledAfterASaveThatWroteSubtasks() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b"))
        viewModel.toggleSubtaskComplete(node("a"))
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        verify(taskCompleter).setComplete(any<Task>(), eq(true), any())
        assertEquals(listOf("a", "b"), tree())
    }

    @Test
    fun theSubtaskSectionIsStillFilledAfterASaveThatWroteNothing() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b"))

        viewModel.setTitle("Renamed")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), tree())
    }

    @Test
    fun nestingHiddenInsideACollapsedSubtaskStillCountsAsNesting() = runTest(testDispatcher) {
        initializeWithSubtasks(
            saved(1, "a").collapsed(),
            saved(2, "b", parent = 1, indent = 1),
        )

        assertEquals(listOf("a"), tree())
        assertTrue(viewModel.state.value.subtasksNested)
    }

    @Test
    fun aSubtaskAddedAndDeletedAgainIsNotStillWaitingTheNextTimeRound() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val added = addSubtask("new")
        viewModel.removeSubtask(added)
        advanceUntilIdle()

        viewModel.setTitle("Renamed")
        viewModel.save()
        advanceUntilIdle()

        assertNull(subtaskTrees.get(added.key))
        assertEquals(listOf("a"), tree())
    }

    @Test
    fun foldingASubtaskThatHasNoRowYetIsStagedUntilItIsCreated() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Parent")
        val outer = addSubtask("outer")
        val inner = addSubtask("inner")
        viewModel.indentSubtask(inner, steps = 1)
        advanceUntilIdle()
        assertEquals(listOf("outer", "  inner"), tree())

        viewModel.toggleSubtaskCollapsed(node("outer"))
        advanceUntilIdle()

        assertEquals(listOf("outer"), tree())
        verify(taskSaver, never()).setCollapsed(any(), any())

        viewModel.save()
        advanceUntilIdle()

        assertTrue(createdRows.values.first { it.title == "outer" }.isCollapsed)
    }

    @Test
    fun openingASubtaskRenamedInTheListEditsTheNameThatRowIsShowing() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        viewModel.setSubtaskTitle(node("a"), "renamed")
        advanceUntilIdle()

        whenever(taskDao.fetchTasks(any<String>())).thenReturn(emptyList())
        buildViewModel(taskId = 1)
        advanceUntilIdle()

        assertEquals("renamed", viewModel.state.value.task.title)
        assertTrue(viewModel.state.value.hasChanges)
    }

    @Test
    fun renamingASubtaskInItsOwnEditorIsNotPutBackByTheTaskAboveIt() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val parent = viewModel
        viewModel.setSubtaskTitle(node("a"), "renamed")
        advanceUntilIdle()

        whenever(taskDao.fetchTasks(any<String>())).thenReturn(emptyList())
        buildViewModel(taskId = 1)
        advanceUntilIdle()
        viewModel.setTitle("renamed twice")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("renamed twice"), tree(parent))
        assertFalse(subtaskTrees.get("uuid-a")!!.titleEdited)
    }

    @Test
    fun closingAnEditorWhoseSaveNeverReachesTheTreeStillTakesItWithIt() =
        runTest(testDispatcher) {
            initializeWithSubtasks(saved(1, "a"), saved(2, "b"))
            viewModel.moveSubtask(fromKey = node("b").key, toKey = node("a").key)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.hasChanges)

            whenever(taskDao.fetch(42L)).thenReturn(
                Task(id = 42, title = "Existing", deletionDate = currentTimeMillis())
            )
            viewModel.onCleared()
            advanceUntilIdle()

            assertTrue(subtaskTrees.isEmpty())
            assertFalse(subtaskTrees.holds(1, "uuid-a"))
        }

    @Test
    fun discardingASubtasksOwnEditorLeavesWhatTheTaskAboveStagedUnderIt() =
        runTest(testDispatcher) {
            initializeWithSubtasks(saved(1, "a"))
            val parent = viewModel
            val added = addSubtask("new")
            viewModel.indentSubtask(added, steps = 1)
            advanceUntilIdle()
            assertEquals(listOf("a", "  new"), tree(parent))

            whenever(taskDao.fetchTasks(any<String>())).thenReturn(emptyList())
            buildViewModel(taskId = 1)
            advanceUntilIdle()
            viewModel.discardChanges()
            advanceUntilIdle()

            assertEquals(listOf("a", "  new"), tree(parent))
            assertNotNull(subtaskTrees.get(added.key))
        }

    @Test
    fun everySubtaskActionIsAccountedForByDiscard() {
        val onScreen = TaskEditViewModel::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }
            .map { it.name }
            .filter { it.contains("ubtask") }
            .toSet()

        assertEquals(
            "A subtask action was added or removed. If it stages anything into the tree, record\n" +
                    "what it displaced with `staged.…` and add it to STAGED - and to\n" +
                    "discardTakesBackEveryKindOfStagedSubtaskEdit. If it does not, add it to\n" +
                    "UNSTAGED with the reason.",
            STAGED.plus(UNSTAGED),
            onScreen,
        )
    }

    @Test
    fun discardTakesBackEveryKindOfStagedSubtaskEdit() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b"), saved(3, "c"))
        val baseline = tree()

        addSubtask("added")
        viewModel.setSubtaskTitle(node("a"), "renamed")
        viewModel.backspaceSubtask(node("renamed"))
        viewModel.restoreSubtask(node("a"))
        viewModel.toggleSubtaskComplete(node("b"))
        viewModel.removeSubtask(node("c"))
        viewModel.indentSubtask(node("b"), steps = 1)
        advanceUntilIdle()
        viewModel.moveSubtask(fromKey = node("b").key, toKey = node("c").key)
        advanceUntilIdle()
        assertNotEquals(baseline, tree())

        viewModel.discardChanges()
        advanceUntilIdle()

        assertEquals(baseline, tree())
        assertFalse(viewModel.state.value.subtasksChanged)
        viewModel.state.value.subtasks.forEach {
            assertFalse(it.node.titleEdited)
            assertFalse(it.node.completionEdited)
            assertFalse(it.node.deleted)
            assertFalse(it.node.moved)
        }
    }

    @Test
    fun aSubtaskAddedInsideAStackedEditorIsCreatedExactlyOnce() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val outer = viewModel
        whenever(taskDao.fetchTasks(any<String>())).thenReturn(emptyList())
        val inner = buildViewModel(taskId = 1, remoteId = "uuid-a")
        advanceUntilIdle()
        addSubtask("inner")

        outer.onCleared()
        inner.onCleared()
        advanceUntilIdle()

        val created = createdRows.values.filter { it.title == "inner" }
        assertEquals(1, created.size)
        assertEquals(1L, created.single().parent)
    }

    @Test
    fun discardingASubtasksOwnEditorThrowsAwayWhatItAddedItself() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val parent = viewModel

        whenever(taskDao.fetchTasks(any<String>())).thenReturn(emptyList())
        buildViewModel(taskId = 1)
        advanceUntilIdle()
        val added = addSubtask("inner")
        assertEquals(listOf("inner"), tree())
        assertEquals(listOf("a"), tree(parent))

        viewModel.discardChanges()
        advanceUntilIdle()

        assertNull(subtaskTrees.get(added.key))
        assertEquals(listOf("a"), tree(parent))
    }

    @Test
    fun movingToAListThatHoldsOneLevelLeavesTheTreeAsItIs() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Parent")
        addSubtask("a")
        val b = addSubtask("b")
        viewModel.indentSubtask(b, steps = 1)
        advanceUntilIdle()
        assertEquals(listOf("a", "  b"), tree())

        viewModel.setList(googleList)
        advanceUntilIdle()

        assertEquals(listOf("a", "  b"), tree())
    }

    @Test
    fun draggingOnAListThatHoldsOneLevelFollowsTheNestingOnScreen() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Parent")
        addSubtask("a")
        val b = addSubtask("b")
        viewModel.indentSubtask(b, steps = 1)
        addSubtask("c")
        advanceUntilIdle()
        assertEquals(listOf("a", "  b", "c"), tree())
        viewModel.setList(googleList)
        advanceUntilIdle()

        viewModel.moveSubtask(fromKey = node("c").key, toKey = node("b").key)
        advanceUntilIdle()

        assertEquals(listOf("a", "  c", "  b"), tree())
    }

    @Test
    fun movingTheTaskToAnotherListTakesItsAddedSubtasksWithIt() = runTest(testDispatcher) {
        buildViewModel(remoteId = "parent-uuid")
        advanceUntilIdle()
        val subtask = addSubtask("Subtask")
        val other = CaldavFilter(calendar = seedCalendar, account = testAccount)

        viewModel.setList(other)
        advanceUntilIdle()

        assertEquals(other, subtaskTrees.get(subtask.key)!!.pending!!.list)
    }

    @Test
    fun anEditorOpenedOnASubtaskSavesWhatItAddedToIt() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val outer = viewModel
        whenever(taskDao.fetchTasks(any<String>())).thenReturn(emptyList())

        val inner = buildViewModel(taskId = 1, remoteId = "uuid-a")
        advanceUntilIdle()
        inner.addSubtask()
        advanceUntilIdle()
        val added = subtaskTrees.get(inner.state.value.focusSubtask!!)!!
        inner.setSubtaskTitle(added, "nested")
        advanceUntilIdle()
        assertTrue(inner.state.value.subtasksChanged)

        inner.save()
        advanceUntilIdle()

        assertEquals(1L, createdRows.values.first { it.title == "nested" }.parent)
        outer.discardChanges()
        advanceUntilIdle()
        assertNull(subtaskTrees.get(added.key))
    }

    @Test
    fun deletingASubtaskDoesNotWriteWhatWasStagedUnderIt() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        whenever(taskDao.fetchTasks(any<String>())).thenReturn(emptyList())
        val inner = buildViewModel(taskId = 1, remoteId = "uuid-a")
        advanceUntilIdle()
        inner.addSubtask()
        advanceUntilIdle()
        inner.setSubtaskTitle(
            subtaskTrees.get(inner.state.value.focusSubtask!!)!!,
            "nested",
        )
        advanceUntilIdle()

        inner.delete()
        advanceUntilIdle()
        inner.onCleared()
        advanceUntilIdle()

        assertTrue(createdRows.values.none { it.title == "nested" })
        assertTrue(subtaskTrees.get("uuid-a")!!.deleted)
    }

    @Test
    fun discardingAnEditorOpenedOnASubtaskTakesBackWhatItStaged() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b", parent = 1, indent = 1))
        val outer = viewModel
        whenever(taskDao.fetchTasks(any<String>())).thenReturn(listOf(saved(2, "b", parent = 1)))
        val inner = buildViewModel(taskId = 1, remoteId = "uuid-a")
        advanceUntilIdle()

        inner.removeSubtask(node("b", inner))
        advanceUntilIdle()
        assertTrue(deleted("b", inner))

        inner.discardChanges()
        advanceUntilIdle()

        assertFalse(subtaskTrees.get("uuid-b")!!.deleted)
        outer.save()
        advanceUntilIdle()
        verify(taskDeleter, never()).markDeleted(any<List<Long>>())
    }

    @Test
    fun discardingAnEditorDoesNotUndoADeletionStagedInAnother() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b", parent = 1, indent = 1))
        val outer = viewModel
        whenever(taskDao.fetchTasks(any<String>())).thenReturn(listOf(saved(2, "b", parent = 1)))
        val inner = buildViewModel(taskId = 1, remoteId = "uuid-a")
        advanceUntilIdle()
        inner.removeSubtask(node("b", inner))
        advanceUntilIdle()
        assertTrue(deleted("b", inner))

        outer.toggleSubtaskComplete(node("a", outer))
        advanceUntilIdle()
        outer.discardChanges()
        advanceUntilIdle()

        assertTrue(deleted("b", inner))
        assertFalse(node("a", outer).completionEdited)
    }

    @Test
    fun aTabThatFindsNothingToNestUnderLeavesNothingToPutBack() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b"))

        viewModel.indentSubtask(node("a"), steps = 1)
        advanceUntilIdle()
        assertEquals(listOf("a", "b"), tree())
        assertFalse(viewModel.state.value.subtasksChanged)

        whenever(taskDao.fetchTasks(any<String>())).thenReturn(listOf(saved(2, "b"), saved(1, "a")))
        refreshes.emit(Unit)
        advanceUntilIdle()
        assertEquals(listOf("b", "a"), tree())

        viewModel.discardChanges()
        advanceUntilIdle()

        assertEquals(listOf("b", "a"), tree())
        assertFalse(viewModel.state.value.subtasksChanged)
    }

    @Test
    fun aSubtaskQueryThatFailsLeavesTheTreeAlone() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b"))
        viewModel.setSubtaskTitle(node("a"), "renamed")
        viewModel.removeSubtask(node("b"))
        advanceUntilIdle()

        whenever(taskDao.fetchTasks(any<String>())).thenThrow(RuntimeException("db locked"))
        refreshes.emit(Unit)
        advanceUntilIdle()

        assertEquals(listOf("renamed", "b"), tree())
        assertTrue(deleted("b"))
    }

    @Test
    fun foldingASubtaskShowsUpBeforeTheWriteHasBeenThrough() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b", parent = 1, indent = 1))

        viewModel.toggleSubtaskCollapsed(node("a"))

        assertEquals(listOf("a"), tree())
        advanceUntilIdle()
        verify(taskSaver).setCollapsed(1L, true)
    }

    @Test
    fun aFoldThatCannotBeWrittenIsPutBack() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b", parent = 1, indent = 1))
        whenever(taskSaver.setCollapsed(any(), any())).thenThrow(RuntimeException("db error"))

        viewModel.toggleSubtaskCollapsed(node("a"))
        advanceUntilIdle()

        assertEquals(listOf("a", "  b"), tree())
    }

    @Test
    fun theTreeSurvivesTheFirstOfTwoEditorsOnTheSameTaskClosing() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val first = viewModel
        val second = buildViewModel(taskId = 42)
        advanceUntilIdle()
        assertEquals(listOf("a"), tree(second))

        first.onCleared()
        advanceUntilIdle()

        assertNotNull(subtaskTrees.get("uuid-a"))
        assertEquals(listOf("a"), tree(second))

        second.onCleared()
        advanceUntilIdle()

        assertNull(subtaskTrees.get("uuid-a"))
    }

    @Test
    fun discardingAnEditorOpenedOnASubtaskTakesBackTheRowsItDragged() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val outer = viewModel
        whenever(taskDao.fetchTasks(any<String>()))
            .thenReturn(listOf(saved(2, "b", parent = 1), saved(3, "c", parent = 1)))
        val inner = buildViewModel(taskId = 1, remoteId = "uuid-a")
        advanceUntilIdle()
        assertEquals(listOf("b", "c"), tree(inner))

        inner.moveSubtask(fromKey = "uuid-c", toKey = "uuid-b")
        advanceUntilIdle()
        assertEquals(listOf("c", "b"), tree(inner))

        inner.discardChanges()
        advanceUntilIdle()

        assertEquals(listOf("b", "c"), tree(inner))
        assertFalse(subtaskTrees.isRearranged("uuid-a"))
        outer.save()
        advanceUntilIdle()
        verify(taskDao, never()).setOrder(any(), anyOrNull())
    }

    @Test
    fun discardingAnEditorLeavesRowsThatWereDraggedUnderWhatItAdded() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val outer = viewModel
        whenever(taskDao.fetchTasks(any<String>())).thenReturn(listOf(saved(2, "b", parent = 1)))
        val inner = buildViewModel(taskId = 1, remoteId = "uuid-a")
        advanceUntilIdle()
        inner.addSubtask()
        advanceUntilIdle()
        val added = subtaskTrees.get(inner.state.value.focusSubtask!!)!!
        inner.setSubtaskTitle(added, "new")
        advanceUntilIdle()
        inner.moveSubtask(fromKey = "uuid-b", toKey = added.key, indent = 1)
        advanceUntilIdle()
        assertEquals(listOf("new", "  b"), tree(inner))

        inner.discardChanges()
        advanceUntilIdle()

        assertNull(subtaskTrees.get(added.key))
        assertEquals(listOf("b"), tree(inner))
    }

    @Test
    fun aDraftSubtaskEditorTakesUpTheRowItsOwnTaskCreatedForIt() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Parent")
        val parent = viewModel
        val subtask = addSubtask("Subtask")
        val draft = buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()
        assertTrue(draft.state.value.isDraft)

        parent.save()
        advanceUntilIdle()
        draft.setTitle("Renamed")
        advanceUntilIdle()

        assertFalse(draft.state.value.isDraft)
        val created = createdRows.values.first { it.title == "Subtask" }
        assertEquals(created.id, draft.state.value.task.id)
        draft.save()
        advanceUntilIdle()
        assertEquals(1, createdRows.values.count { it.title in setOf("Subtask", "Renamed") })
        verify(taskSaver).save(check { assertEquals("Renamed", it.title) }, anyOrNull(), any(), any())
    }

    @Test
    fun movingToAListThatHoldsOneLevelAndBackLeavesTheNestingAlone() = runTest(testDispatcher) {
        initializeWithSubtasks(
            saved(1, "a"),
            saved(2, "b", parent = 1, indent = 1),
            saved(3, "c", parent = 2, indent = 2),
        )
        val caldav = viewModel.state.value.list!!
        val singleLevel = CaldavFilter(
            calendar = seedCalendar,
            account = CaldavAccount(uuid = "acct-1", accountType = TYPE_GOOGLE_TASKS),
        )

        viewModel.setList(singleLevel)
        advanceUntilIdle()
        assertEquals(listOf("a", "  b", "    c"), tree())

        viewModel.setList(caldav)
        advanceUntilIdle()

        assertEquals(listOf("a", "  b", "    c"), tree())
        viewModel.save()
        advanceUntilIdle()
        verify(taskDao, never()).setParent(any(), any())
    }

    @Test
    fun aDragOnAListThatHoldsOneLevelKeepsTheNestingUntilTheSave() = runTest(testDispatcher) {
        initializeWithSubtasks(
            saved(1, "a"),
            saved(2, "b", parent = 1, indent = 1),
            saved(3, "c"),
        )
        viewModel.setList(
            CaldavFilter(
                calendar = seedCalendar,
                account = CaldavAccount(uuid = "acct-1", accountType = TYPE_GOOGLE_TASKS),
            )
        )
        advanceUntilIdle()
        assertEquals(listOf("a", "  b", "c"), tree())

        viewModel.moveSubtask(fromKey = "uuid-c", toKey = "uuid-a")
        advanceUntilIdle()

        assertEquals(listOf("c", "a", "  b"), tree())
    }

    @Test
    fun nothingReachesPastTheDatabaseWhileTheWriteIsStillOpen() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val duringTransaction = mutableListOf<String>()
        taskDao.stub {
            onBlocking { inTransaction<Any?>(any()) } doSuspendableAnswer { invocation ->
                inTransaction = true
                try {
                    @Suppress("UNCHECKED_CAST")
                    (invocation.arguments[0] as suspend () -> Any?).invoke()
                } finally {
                    inTransaction = false
                }
            }
        }
        whenever(refreshBroadcaster.broadcastRefresh()).thenAnswer {
            if (inTransaction) duringTransaction.add("refresh")
            Unit
        }
        whenever(taskDeleter.markDeleted(any<List<Long>>())).thenAnswer {
            if (inTransaction) duringTransaction.add("delete")
            Unit
        }
        whenever(alarmService.synchronizeAlarms(any(), any())).thenAnswer {
            if (inTransaction) duringTransaction.add("alarms")
            true
        }
        whenever(appPreferences.defaultAlarms())
            .thenReturn(listOf(Alarm(time = ONE_HOUR, type = Alarm.TYPE_RANDOM)))
        addSubtask("added")
        viewModel.removeSubtask(node("a"))
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), duringTransaction)
        verify(taskDeleter).markDeleted(listOf(1L))
        verify(alarmService).synchronizeAlarms(any(), any())
    }

    @Test
    fun aWriteThatFailsInsideTheTransactionLeavesTheTreeToTryAgain() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        addSubtask("added")
        advanceUntilIdle()
        taskDao.stub {
            onBlocking { inTransaction<Any?>(any()) } doSuspendableAnswer {
                throw RuntimeException("rolled back")
            }
        }

        viewModel.save()
        advanceUntilIdle()

        assertTrue(createdRows.values.none { it.title == "added" })
        val staged = viewModel.state.value.subtasks.map { it.node }
        assertEquals(listOf("a", "added"), staged.map { it.title })
        assertTrue(staged.last().isNew)
        assertTrue(viewModel.state.value.hasChanges)

        taskDao.stub {
            onBlocking { inTransaction<Any?>(any()) } doSuspendableAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                (invocation.arguments[0] as suspend () -> Any?).invoke()
            }
        }
        viewModel.save()
        advanceUntilIdle()
        assertEquals(1, createdRows.values.count { it.title == "added" })
    }

    @Test
    fun backspacingAnAddedSubtaskAwayLeavesWhatWasDraggedUnderIt() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val added = addSubtask("new")
        viewModel.moveSubtask(fromKey = "uuid-a", toKey = added.key, indent = 1)
        advanceUntilIdle()
        assertEquals(listOf("new", "  a"), tree())
        viewModel.setSubtaskTitle(node("new"), "")
        advanceUntilIdle()

        viewModel.backspaceSubtask(subtaskTrees.get(added.key)!!)
        advanceUntilIdle()

        assertEquals(listOf("a"), tree())
        assertNull(subtaskTrees.get(added.key))
    }

    @Test
    fun discardingADraftSubtaskTakesWhatWasAddedInsideItAndLeavesTheRest() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val parent = viewModel
        val subtask = addSubtask("Subtask")
        val draft = buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()
        assertTrue(draft.state.value.isDraft)
        draft.addSubtask()
        advanceUntilIdle()
        val nested = subtaskTrees.get(draft.state.value.focusSubtask!!)!!
        draft.setSubtaskTitle(nested, "nested")
        advanceUntilIdle()
        draft.moveSubtask(fromKey = "uuid-a", toKey = nested.key)
        advanceUntilIdle()

        draft.discardChanges()
        advanceUntilIdle()

        assertNull(subtaskTrees.get(subtask.key))
        assertNull(subtaskTrees.get(nested.key))
        assertEquals(listOf("a"), tree(parent))
        parent.save()
        advanceUntilIdle()
        assertTrue(createdRows.values.none { it.title in setOf("Subtask", "nested") })
    }

    @Test
    fun aDraftEditorTakesUpItsRowEvenWhileTheNodeIsStillThere() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Parent")
        val subtask = addSubtask("Subtask")
        val draft = buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()

        subtaskTrees.takePending(subtask.key)
        subtaskTrees.settle(mapOf(subtask.key to Task(id = 99, title = "Subtask", remoteId = subtask.key)))
        draft.setTitle("Renamed")
        advanceUntilIdle()

        assertFalse(draft.state.value.isDraft)
        assertEquals(99L, draft.state.value.task.id)
        assertNull(subtaskTrees.get(subtask.key)?.pending)
    }

    @Test
    fun aRowThatLandedWhileTheSaveWasStartingIsNotRenumberedWithout() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"), saved(2, "b"))
        viewModel.setSubtaskTitle(node("a"), "renamed")
        advanceUntilIdle()
        taskDao.stub {
            onBlocking { inTransaction<Any?>(any()) } doSuspendableAnswer { invocation ->
                whenever(taskDao.fetchTasks(any<String>()))
                    .thenReturn(listOf(saved(1, "a"), saved(3, "c"), saved(2, "b")))
                @Suppress("UNCHECKED_CAST")
                (invocation.arguments[0] as suspend () -> Any?).invoke()
            }
        }
        createdRows[3] = saved(3, "c").task

        viewModel.save()
        advanceUntilIdle()

        verify(taskDao, never()).setOrder(any(), anyOrNull())
        verify(dirtyDao, never()).setDirty(any(), any())
    }

    @Test
    fun aSubtaskOnItsWayOutTakesWhatIsNestedInsideItOnAnyList() =
        runTest(testDispatcher) {
            initializeWithSubtasks(
                saved(1, "a"),
                saved(2, "b", parent = 1, indent = 1),
                saved(3, "c", parent = 2, indent = 2),
            )
            viewModel.setList(
                CaldavFilter(
                    calendar = seedCalendar,
                    account = CaldavAccount(uuid = "acct-1", accountType = TYPE_GOOGLE_TASKS),
                )
            )
            advanceUntilIdle()
            assertEquals(listOf("a", "  b", "    c"), tree())

            viewModel.removeSubtask(node("a"))
            advanceUntilIdle()

            assertEquals(listOf("a"), tree())

            viewModel.save()
            advanceUntilIdle()
            verify(taskDeleter).markDeleted(check<List<Long>> {
                assertEquals(listOf(1L, 2L, 3L), it)
            })
        }

    @Test
    fun aDraftEditorTakesTheRowsPlaceInTheTreeAsWellAsItsId() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Parent")
        val parent = viewModel
        val subtask = addSubtask("Subtask")
        val draft = buildViewModel(remoteId = subtask.key)
        advanceUntilIdle()

        parent.save()
        advanceUntilIdle()
        draft.setTitle("Renamed")
        advanceUntilIdle()
        reset(taskSaver)
        draft.save()
        advanceUntilIdle()

        val created = createdRows.values.first { it.title == "Subtask" }
        verify(taskSaver).save(
            check {
                assertEquals("Renamed", it.title)
                assertEquals(created.parent, it.parent)
                assertEquals(created.order, it.order)
            },
            anyOrNull(),
            any(),
            any(),
        )
    }

    @Test
    fun aSubtaskDeletedFromItsOwnEditorStaysDeletedWhenTheTaskAboveIsDiscarded() =
        runTest(testDispatcher) {
            initializeWithSubtasks(saved(1, "a"), saved(2, "b"))
            val outer = viewModel
            whenever(taskDao.fetchTasks(any<String>())).thenReturn(emptyList())
            val inner = buildViewModel(taskId = 1, remoteId = "uuid-a")
            advanceUntilIdle()

            inner.delete()
            advanceUntilIdle()
            assertTrue(deleted("a", outer))

            whenever(taskDao.fetchTasks(any<String>()))
                .thenReturn(listOf(saved(1, "a"), saved(2, "b")))
            outer.discardChanges()
            advanceUntilIdle()

            assertTrue(deleted("a", outer))
            outer.onCleared()
            advanceUntilIdle()
            verify(taskDeleter).markDeleted(check<List<Long>> { assertEquals(listOf(1L), it) })
        }

    @Test
    fun theParentsRemoteIdIsLookedUpOnceForTheWholeRun() = runTest(testDispatcher) {
        initializeWithSubtasks()
        whenever(caldavDao.getTask(any())).thenReturn(CaldavTask(id = 9, task = 42, calendar = "cal-1"))
        whenever(caldavDao.getRemoteIdForTask(any())).thenReturn(null)
        repeat(3) { addSubtask("added $it") }

        viewModel.save()
        advanceUntilIdle()

        verify(caldavDao, times(1)).getRemoteIdForTask(42L)
    }

    @Test
    fun aDraftSubtaskThatNoLongerExistsIsNotOfferedAsANewTask() = runTest(testDispatcher) {
        val restored = buildViewModel(remoteId = "uuid-gone", isSubtaskDraft = true)
        advanceUntilIdle()

        assertTrue(restored.state.value.deleted)
        assertFalse(restored.state.value.hasChanges)
    }

    @Test
    fun aNewTaskDestinationIsStillOfferedAnEditor() = runTest(testDispatcher) {
        val fresh = buildViewModel(remoteId = "uuid-new-task")
        advanceUntilIdle()

        assertFalse(fresh.state.value.deleted)
        assertTrue(fresh.state.value.isNew)
    }

    @Test
    fun theTreeAnEditorGivesUpOnLoadIsNotLeftBehind() = runTest(testDispatcher) {
        whenever(taskDao.fetch(42L)).thenReturn(Task(id = 42, title = "Existing", remoteId = "uuid-42"))
        whenever(caldavDao.getTask(42L)).thenReturn(null)
        buildViewModel(taskId = 42)
        advanceUntilIdle()

        viewModel.onCleared()
        advanceUntilIdle()

        assertTrue(subtaskTrees.isEmpty())
        assertFalse(subtaskTrees.holds(42, "uuid-42"))
    }

    @Test
    fun aTickMadeOnADraftSurvivesOpeningItsOwnEditor() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Parent")
        val subtask = addSubtask("Subtask")
        val parent = viewModel
        parent.toggleSubtaskComplete(subtask)
        advanceUntilIdle()
        assertTrue(parent.state.value.subtasks.single().completed)

        val draft = buildViewModel(remoteId = subtask.key, isSubtaskDraft = true)
        advanceUntilIdle()

        assertTrue(draft.state.value.task.isCompleted)
        assertTrue(subtaskTrees.get(subtask.key)!!.completed)
        assertTrue(parent.state.value.subtasks.single().completed)
    }

    @Test
    fun openingASubtaskDoesNotDisturbATickStagedOneLevelUp() = runTest(testDispatcher) {
        val completed = TaskContainer(
            task = Task(id = 1, title = "a", parent = 42, remoteId = "uuid-a", completionDate = 1L),
        )
        initializeWithSubtasks(completed)
        val parent = viewModel
        parent.toggleSubtaskComplete(node("a"))
        advanceUntilIdle()
        assertFalse(parent.state.value.subtasks.single().completed)

        whenever(taskDao.fetchTasks(any<String>())).thenReturn(emptyList())
        val inner = buildViewModel(taskId = 1, remoteId = "uuid-a")
        advanceUntilIdle()

        assertFalse(subtaskTrees.get("uuid-a")!!.completed)
        assertTrue(subtaskTrees.get("uuid-a")!!.completionEdited)
        assertFalse(inner.state.value.hasChanges)
        assertFalse(parent.state.value.subtasks.single().completed)
    }

    @Test
    fun anAddedSubtaskIsCreatedWithTheDefaultRingMode() = runTest(testDispatcher) {
        whenever(appPreferences.defaultRingMode()).thenReturn(2)
        initializeNew()
        viewModel.setTitle("Parent")

        val subtask = addSubtask("Subtask")

        assertEquals(2, subtaskTrees.get(subtask.key)!!.task.ringFlags)
    }

    @Test
    fun tickingASubtaskCascadesThroughTheNestingOnEveryList() = runTest(testDispatcher) {
        initializeWithSubtasks(
            saved(1, "a"),
            saved(2, "b", parent = 1, indent = 1),
        )
        viewModel.setList(
            CaldavFilter(
                calendar = seedCalendar,
                account = CaldavAccount(uuid = "acct-1", accountType = TYPE_GOOGLE_TASKS),
            )
        )
        advanceUntilIdle()
        assertEquals(listOf("a", "  b"), tree())

        viewModel.toggleSubtaskComplete(node("a"))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.subtasks.first { it.node.title == "a" }.completed)
        assertTrue(viewModel.state.value.subtasks.first { it.node.title == "b" }.completed)
        assertFalse(subtaskTrees.get("uuid-b")!!.completionEdited)
    }

    @Test
    fun nestingASubtaskUnderAFoldedRowLeavesItFolded() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a").collapsed(), saved(2, "b"))

        viewModel.indentSubtask(node("b"), 1)
        advanceUntilIdle()

        assertEquals(listOf("a"), tree())
        assertEquals("uuid-a", subtaskTrees.get("uuid-b")!!.parentKey)
        assertTrue(subtaskTrees.get("uuid-a")!!.task.isCollapsed)
    }

    @Test
    fun draggingASubtaskIntoAFoldedRowLeavesItFolded() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a").collapsed(), saved(2, "b"), saved(3, "c"))

        viewModel.moveSubtask(fromKey = "uuid-c", toKey = "uuid-b", indent = 1)
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), tree())
        assertEquals("uuid-a", subtaskTrees.get("uuid-c")!!.parentKey)
        assertTrue(subtaskTrees.get("uuid-a")!!.task.isCollapsed)
    }

    @Test
    fun completingADraftFromItsOwnEditorSurvivesThatEditorClosing() =
        runTest(testDispatcher) {
            initializeWithSubtasks()
            val parent = viewModel
            val subtask = addSubtask("Subtask")
            val draft = buildViewModel(remoteId = subtask.key, isSubtaskDraft = true)
            advanceUntilIdle()

            draft.markComplete()
            advanceUntilIdle()
            draft.onCleared()
            advanceUntilIdle()

            val node = subtaskTrees.get(subtask.key)!!
            assertEquals(0L, node.task.completionDate)
            assertTrue(node.completionEdited)
            assertTrue(node.completed)

            parent.save()
            advanceUntilIdle()

            verify(taskCompleter).setComplete(any<Task>(), eq(true), any())
        }

    @Test
    fun completingADraftGoesThroughTheCompleterRatherThanStampingTheRow() =
        runTest(testDispatcher) {
            initializeWithSubtasks()
            val parent = viewModel
            val subtask = addSubtask("Subtask")
            val draft = buildViewModel(remoteId = subtask.key, isSubtaskDraft = true)
            advanceUntilIdle()

            draft.markComplete()
            advanceUntilIdle()

            val node = subtaskTrees.get(subtask.key)!!
            assertEquals(0L, node.task.completionDate)
            assertTrue(node.completionEdited)
            assertTrue(node.completed)

            parent.save()
            advanceUntilIdle()

            verify(taskCompleter).setComplete(any<Task>(), eq(true), any())
        }

    @Test
    fun aSubtaskAddedWhileTheSaveWasRunningCanStillBeDiscarded() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        viewModel.setSubtaskTitle(node("a"), "renamed")
        advanceUntilIdle()
        var late: String? = null
        taskDao.stub {
            onBlocking { inTransaction<Any?>(any()) } doSuspendableAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                val result = (invocation.arguments[0] as suspend () -> Any?).invoke()
                if (late == null) {
                    viewModel.addSubtask()
                    late = viewModel.state.value.focusSubtask
                }
                result
            }
        }

        viewModel.save()
        advanceUntilIdle()
        viewModel.discardChanges()
        advanceUntilIdle()

        assertNotNull(late)
        assertNull(subtaskTrees.get(late!!))
        assertTrue(createdRows.values.none { it.id > 1 && it.title.isNullOrBlank() })
    }

    @Test
    fun aTitleTypedIntoTheListWhileTheSubtaskWasSavingIsNotClearedByIt() = runTest(testDispatcher) {
        initializeWithSubtasks(saved(1, "a"))
        val parent = viewModel
        parent.setSubtaskTitle(node("a", parent), "typed above")
        advanceUntilIdle()

        whenever(taskDao.fetchTasks(any<String>())).thenReturn(emptyList())
        buildViewModel(taskId = 1)
        advanceUntilIdle()
        val subtaskEditor = viewModel
        var typedLate = false
        whenever(taskSaver.save(any(), anyOrNull(), any(), any())).doSuspendableAnswer {
            if (!typedLate) {
                typedLate = true
                parent.setSubtaskTitle(node("typed above", parent), "typed later")
            }
            Unit
        }

        subtaskEditor.save()
        advanceUntilIdle()

        assertTrue(typedLate)
        assertEquals("typed later", subtaskTrees.get("uuid-a")?.stagedTitle)
        assertEquals(listOf("typed later"), tree(parent))
    }
}
