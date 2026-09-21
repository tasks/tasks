package org.tasks.data

import com.todoroo.astrid.alarms.AlarmService
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.DatabaseTest
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.compose.pickers.DUE_DATE
import org.tasks.filters.SubtaskFilter
import org.tasks.preferences.SubtaskQueryPreferences
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter

class SubtaskTreeWriterTest : DatabaseTest() {
    private val taskDao = db.taskDao()
    private val caldavDao = db.caldavDao()
    private val googleTaskDao = db.googleTaskDao()
    private val dirtyDao = db.dirtyDao()
    private val taskCompleter by lazy {
        TaskCompleter(
            taskDao = taskDao,
            taskSaver = taskSaver,
            notifier = mock(),
            refreshBroadcaster = mock(),
            repeatTaskHelper = mock(),
            caldavDao = caldavDao,
            calendarHelper = mock(),
            completionDao = db.completionDao(),
            soundPlayer = mock(),
        )
    }
    private val taskDeleter = TaskDeleter(
        deletionDao = db.deletionDao(),
        taskDao = taskDao,
        caldavDao = caldavDao,
        refreshBroadcaster = mock(),
        vtodoCache = mock(),
        tasksPreferences = mock(),
        taskCleanup = object : org.tasks.service.TaskCleanup {},
    )

    private val taskSaver = TaskSaver(
        taskDao = taskDao,
        refreshBroadcaster = mock(),
        notifier = mock(),
        locationService = mock(),
        timerPlugin = mock(),
        backgroundWork = mock(),
        caldavDao = caldavDao,
    )

    private val alarmService: AlarmService = mock()

    private fun writerWith(completer: TaskCompleter = taskCompleter) = SubtaskTreeWriter(
        taskDao = taskDao,
        caldavDao = caldavDao,
        googleTaskDao = googleTaskDao,
        tagDao = db.tagDao(),
        dirtyDao = dirtyDao,
        alarmService = alarmService,
        taskCompleter = completer,
        taskDeleter = taskDeleter,
        taskSaver = taskSaver,
        refreshBroadcaster = mock(),
    )

    private val writer by lazy { writerWith() }

    private val trees = SubtaskTrees()

    private var accountType = TYPE_CALDAV

    private var parent = Task()
    private val rootKey get() = subtaskKey(parent)
    private val list
        get() = CaldavFilter(
            calendar = CaldavCalendar(uuid = CALENDAR, account = ACCOUNT),
            account = CaldavAccount(uuid = ACCOUNT, accountType = accountType),
        )

    @Before
    fun setUp() = runBlocking {
        caldavDao.insert(CaldavAccount(uuid = ACCOUNT, accountType = TYPE_CALDAV))
        caldavDao.insert(CaldavCalendar(uuid = CALENDAR, account = ACCOUNT))
        parent = newTask("Parent", parent = 0)
    }

    private suspend fun newTask(
        title: String,
        parent: Long,
        order: Long? = null,
        completed: Boolean = false,
    ): Task {
        val task = Task(
            title = title,
            parent = parent,
            order = order,
            remoteId = "uuid-$title",
            completionDate = if (completed) COMPLETED_AT else 0,
        )
        taskDao.createNew(task)
        caldavDao.insert(
            task = task,
            caldavTask = CaldavTask(task = task.id, calendar = CALENDAR, remoteId = "uuid-$title"),
            addToTop = false,
        )
        return task
    }

    private suspend fun useGoogleTasks() = useAccountType(TYPE_GOOGLE_TASKS)

    private suspend fun useAccountType(type: Int) {
        accountType = type
        val account = caldavDao.getAccountByUuid(ACCOUNT) ?: error("no account to move")
        caldavDao.update(account.copy(accountType = type))
        assertEquals(type, caldavDao.getAccountByUuid(ACCOUNT)!!.accountType)
    }

    private suspend fun rows(): List<TaskContainer> =
        taskDao.fetchTasks(
            subtaskQuery(
                parentId = parent.id,
                isGoogleTasks = accountType == TYPE_GOOGLE_TASKS,
            )
        )

    private suspend fun tree(): List<String> =
        rows().map { "${"  ".repeat(it.indent)}${it.title}" }

    private suspend fun stage() {
        trees.merge(rootKey, parent.id, rows())
    }

    private suspend fun write(): Boolean =
        writer.write(trees, rootKey, parent, list, untitled = "(No title)").wrote

    private fun node(title: String): SubtaskNode =
        trees.rowsOf(rootKey).first { it.node.title == title }.node

    @Test
    fun subtasksAreReadInTheOrderTheyAreStoredIn() = runBlocking {
        newTask("c", parent = parent.id, order = 3)
        newTask("a", parent = parent.id, order = 1)
        newTask("b", parent = parent.id, order = 2)

        assertEquals(listOf("a", "b", "c"), tree())
    }

    @Test
    fun subtasksAreReadInStoredOrderOnAGoogleListToo() = runBlocking {
        useGoogleTasks()
        newTask("c", parent = parent.id, order = 3)
        newTask("a", parent = parent.id, order = 1)
        newTask("b", parent = parent.id, order = 2)

        assertEquals(listOf("a", "b", "c"), tree())
    }

    @Test
    fun writeNothingWhenNothingMoved() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = parent.id, order = 2)
        stage()

        val wrote = write()

        assertFalse(wrote)
        assertEquals(listOf("a", "b"), tree())
        assertEquals(1L, taskDao.fetch(a.id)!!.order)
        assertEquals(2L, taskDao.fetch(b.id)!!.order)
    }

    @Test
    fun reorderSubtasks() = runBlocking {
        newTask("a", parent = parent.id, order = 1)
        newTask("b", parent = parent.id, order = 2)
        newTask("c", parent = parent.id, order = 3)
        stage()

        trees.move(key = node("c").key, parentKey = rootKey, after = null)
        write()

        assertEquals(listOf("c", "a", "b"), tree())
    }

    @Test
    fun nestSubtaskUnderAnother() = runBlocking {
        newTask("a", parent = parent.id, order = 1)
        newTask("b", parent = parent.id, order = 2)
        stage()

        trees.indent(node("b").key)
        write()

        assertEquals(listOf("a", "  b"), tree())
    }

    @Test
    fun unnestSubtaskThatHasChildrenOfItsOwn() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = a.id, order = 2)
        newTask("c", parent = b.id, order = 3)
        stage()
        assertEquals(listOf("a", "  b", "    c"), tree())

        trees.outdent(rootKey, node("b").key)
        write()

        assertEquals(listOf("a", "b", "  c"), tree())
    }

    @Test
    fun createAnAddedSubtaskBetweenTwoThatExist() = runBlocking {
        newTask("a", parent = parent.id, order = 1)
        newTask("b", parent = parent.id, order = 2)
        stage()

        val added = trees.add(rootKey, Task(title = "new", remoteId = "uuid-new"), list)
        trees.move(key = added.key, parentKey = rootKey, after = node("a").key)
        write()

        assertEquals(listOf("a", "new", "b"), tree())
    }

    @Test
    fun createNestedSubtasksUnderTheirOwnParent() = runBlocking {
        stage()
        val outer = trees.add(rootKey, Task(title = "outer", remoteId = "uuid-outer"), list)
        val inner = trees.add(rootKey, Task(title = "inner", remoteId = "uuid-inner"), list)
        trees.indent(inner.key)

        write()

        assertEquals(listOf("outer", "  inner"), tree())
    }

    @Test
    fun flattenOnAListThatHoldsOneLevel() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        newTask("b", parent = a.id, order = 2)
        stage()
        assertEquals(listOf("a", "  b"), tree())
        useGoogleTasks()

        write()

        assertEquals(listOf("a", "b"), tree())
    }

    @Test
    fun googleTaskSubtasksReorderByPosition() = runBlocking {
        useGoogleTasks()
        newTask("a", parent = parent.id, order = 0)
        newTask("b", parent = parent.id, order = 1)
        newTask("c", parent = parent.id, order = 2)
        stage()

        trees.move(key = node("c").key, parentKey = rootKey, after = null)
        write()

        assertEquals(listOf("c", "a", "b"), tree())
    }

    @Test
    fun aCollapsedSubtaskStillBringsItsOwnAlong() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        newTask("b", parent = a.id, order = 2)
        taskDao.setCollapsed(listOf(a.id), true)
        stage()

        assertEquals(listOf("a", "  b"), trees.rowsOf(rootKey).map {
            "${"  ".repeat(it.indent)}${it.node.title}"
        })
        assertEquals(listOf("a"), trees.rowsOf(rootKey).visible().map { it.node.title })
    }

    @Test
    fun aTitleTypedIntoTheListIsWrittenToTheRow() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        stage()

        trees.setTitle(node("a").key, "renamed")
        val wrote = write()

        assertTrue(wrote)
        assertEquals("renamed", taskDao.fetch(a.id)!!.title)
    }

    @Test
    fun aTitleRubbedOutInTheListIsWrittenAsThePlaceholder() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        stage()

        trees.setTitle(node("a").key, "")
        val wrote = write()

        assertTrue(wrote)
        assertEquals("(No title)", taskDao.fetch(a.id)!!.title)
    }

    @Test
    fun aRowAlreadyUnderThePlaceholderIsNotWrittenAgain() = runBlocking {
        val a = newTask("(No title)", parent = parent.id, order = 1)
        stage()

        trees.setTitle(node("(No title)").key, "")
        val wrote = write()

        assertFalse(wrote)
        assertEquals("(No title)", taskDao.fetch(a.id)!!.title)
    }

    @Test
    fun aTitleTypedIntoTheListSurvivesTheQueryComingBack() = runBlocking {
        newTask("a", parent = parent.id, order = 1)
        stage()
        val key = node("a").key
        trees.setTitle(key, "renamed")

        stage()

        assertEquals("renamed", trees.get(key)!!.title)
    }

    @Test
    fun anUntitledSubtaskWithSubtasksOfItsOwnIsKeptUnderAPlaceholder() = runBlocking {
        stage()
        val outer = trees.add(rootKey, Task(remoteId = "uuid-outer"), list)
        val inner = trees.add(rootKey, Task(title = "inner", remoteId = "uuid-inner"), list)
        trees.indent(inner.key)

        write()

        assertEquals(listOf("(No title)", "  inner"), tree())
    }

    @Test
    fun anUntitledSubtaskWithNothingInsideItIsNotCreated() = runBlocking {
        stage()
        trees.add(rootKey, Task(remoteId = "uuid-blank"), list)

        write()

        assertEquals(emptyList<String>(), tree())
    }

    @Test
    fun nestingASubtaskPointsItsRemoteParentAtTheOneAboveIt() = runBlocking {
        newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = parent.id, order = 2)
        stage()

        trees.indent(node("b").key)
        write()

        assertEquals("uuid-a", caldavDao.getTask(b.id)!!.remoteParent)
    }

    @Test
    fun microsoftHierarchyIsLeftForItsOwnSyncToWorkOut() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = a.id, order = 2)
        stage()
        assertEquals(listOf("a", "  b"), tree())
        useAccountType(TYPE_MICROSOFT)

        write()

        assertEquals(listOf("a", "b"), tree())
        assertNull(caldavDao.getTask(b.id)!!.remoteParent)
    }

    @Test
    fun stagedCompletionIsAppliedThroughTheCompleter() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        stage()

        trees.setCompleted(node("a").key, true)
        val wrote = write()

        assertTrue(wrote)
        assertTrue(taskDao.fetch(a.id)!!.isCompleted)
    }

    @Test
    fun completingASubtaskCompletesWhatIsNestedInsideIt() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = a.id, order = 1)
        stage()

        trees.setCompleted(node("a").key, true)
        write()

        assertTrue(taskDao.fetch(a.id)!!.isCompleted)
        assertTrue(taskDao.fetch(b.id)!!.isCompleted)
        assertFalse(taskDao.fetch(parent.id)!!.isCompleted)
    }

    @Test
    fun unTickingANestedSubtaskReopensTheOnesHoldingIt() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = a.id, order = 1)
        val c = newTask("c", parent = parent.id, order = 2)
        taskCompleter.setComplete(parent, true)
        parent = taskDao.fetch(parent.id)!!
        stage()

        trees.setCompleted(node("b").key, false)
        write()

        assertFalse(taskDao.fetch(b.id)!!.isCompleted)
        assertFalse(taskDao.fetch(a.id)!!.isCompleted)
        assertFalse(taskDao.fetch(parent.id)!!.isCompleted)
        assertTrue(taskDao.fetch(c.id)!!.isCompleted)
    }

    @Test
    fun unTickingASubtaskAndTickingItAgainLeavesTheCompletionDatesAlone() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1, completed = true)
        val b = newTask("b", parent = a.id, order = 1, completed = true)
        stage()

        trees.setCompleted(node("b").key, false)
        trees.setCompleted(node("b").key, true)
        val wrote = write()

        assertFalse(wrote)
        assertEquals(COMPLETED_AT, taskDao.fetch(a.id)!!.completionDate)
        assertEquals(COMPLETED_AT, taskDao.fetch(b.id)!!.completionDate)
    }

    @Test
    fun aSubtaskCompletedElsewhereIsLeftCompleted() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        stage()

        taskCompleter.setComplete(a, true)
        stage()
        val wrote = write()

        assertFalse(wrote)
        assertTrue(taskDao.fetch(a.id)!!.isCompleted)
        assertFalse(taskDao.fetch(parent.id)!!.isCompleted)
    }

    @Test
    fun aTickTakenBackBeforeTheSaveWritesNothing() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        stage()

        trees.setCompleted(node("a").key, true)
        trees.setCompleted(node("a").key, false)
        val wrote = write()

        assertFalse(wrote)
        assertFalse(taskDao.fetch(a.id)!!.isCompleted)
    }

    @Test
    fun addingAnOutstandingSubtaskToACompletedTaskReopensIt() = runBlocking {
        taskCompleter.setComplete(parent, true)
        parent = taskDao.fetch(parent.id)!!
        stage()

        trees.add(rootKey, Task(title = "new", remoteId = "uuid-new"), list)
        write()

        assertEquals(listOf("new"), tree())
        assertFalse(rows().single().task.isCompleted)
        assertFalse(taskDao.fetch(parent.id)!!.isCompleted)
    }

    @Test
    fun addingAnOutstandingSubtaskInsideACompletedOneLeavesItsSiblingsDone() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = a.id, order = 1)
        taskCompleter.setComplete(parent, true)
        parent = taskDao.fetch(parent.id)!!
        stage()

        trees.add(node("a").key, Task(title = "new", remoteId = "uuid-new"), list)
        write()

        assertFalse(taskDao.fetch(a.id)!!.isCompleted)
        assertFalse(taskDao.fetch(parent.id)!!.isCompleted)
        assertTrue(taskDao.fetch(b.id)!!.isCompleted)
    }

    @Test
    fun aStagedDeletionTakesTheWholeSubtreeWithIt() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = a.id, order = 1)
        newTask("c", parent = parent.id, order = 2)
        stage()

        trees.delete(node("a").key)
        val wrote = write()

        assertTrue(wrote)
        assertEquals(listOf("c"), tree())
        assertTrue(taskDao.fetch(a.id)!!.isDeleted)
        assertTrue(taskDao.fetch(b.id)!!.isDeleted)
    }

    @Test
    fun aSubtaskDraggedOutOfADeletedOneIsNotSweptUpWithIt() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = a.id, order = 1)
        stage()

        trees.move(key = node("b").key, parentKey = rootKey, after = node("a").key)
        trees.delete(node("a").key)
        write()

        assertEquals(listOf("b"), tree())
        assertTrue(taskDao.fetch(a.id)!!.isDeleted)
        assertFalse(taskDao.fetch(b.id)!!.isDeleted)
        assertEquals(parent.id, taskDao.fetch(b.id)!!.parent)
    }

    @Test
    fun aSubtaskDeletedBeforeItWasEverSavedIsSimplyNotCreated() = runBlocking {
        newTask("a", parent = parent.id, order = 1)
        stage()
        val added = trees.add(rootKey, Task(title = "b", remoteId = "uuid-b"), list = null)

        trees.delete(added.key)
        val wrote = write()

        assertFalse(wrote)
        assertEquals(listOf("a"), tree())
    }

    @Test
    fun deletingASubtaskLeavesItsSiblingsSortKeysAlone() = runBlocking {
        newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = parent.id, order = 2)
        stage()

        trees.delete(node("a").key)
        write()

        assertEquals(listOf("b"), tree())
        assertEquals(2L, taskDao.fetch(b.id)!!.order)
    }

    @Test
    fun aCompletedSubtaskIsReadWhereItIsStoredRatherThanAtTheBottom() = runBlocking {
        newTask("a", parent = parent.id, order = 1)
        newTask("b", parent = parent.id, order = 2, completed = true)
        newTask("c", parent = parent.id, order = 3)

        stage()

        assertEquals(listOf("a", "b", "c"), trees.rowsOf(rootKey).map { it.node.title })
    }

    @Test
    fun addingASubtaskLeavesACompletedOneWhereItWas() = runBlocking {
        newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = parent.id, order = 2, completed = true)
        newTask("c", parent = parent.id, order = 3)
        stage()

        trees.add(rootKey, Task(title = "new", remoteId = "uuid-new"), list)
        write()

        assertEquals(listOf("a", "b", "c", "new"), tree())
        assertEquals(2L, taskDao.fetch(b.id)!!.order)
    }

    @Test
    fun anAddedSubtaskIsCreatedWithTheTagsAndRemindersItWasGiven() = runBlocking {
        val tag = TagData(name = "work", remoteId = "tag-work")
        db.tagDataDao().insert(tag)
        stage()
        val alarm = Alarm(time = 1_000L, type = TYPE_DATE_TIME)

        val added = trees.add(rootKey, Task(title = "new", remoteId = "uuid-new"), list)
        trees.update(added.key) {
            it.copy(pending = it.pending!!.copy(tags = listOf(tag), alarms = persistentSetOf(alarm)))
        }
        write()

        val created = taskDao.fetch(rows().first { it.title == "new" }.id)!!
        assertEquals(listOf("work"), db.tagDataDao().getTagDataForTask(created.id).map { it.name })
        verify(alarmService).synchronizeAlarms(
            eq(created.id),
            check { assertEquals(setOf(TYPE_DATE_TIME to 1_000L), it.map { a -> a.type to a.time }.toSet()) },
        )
        Unit
    }

    @Test
    fun anAddedSubtaskOnAGoogleListIsCreatedThereAndFlaggedForTheSync() = runBlocking {
        useGoogleTasks()
        newTask("a", parent = parent.id, order = 0)
        stage()

        trees.add(rootKey, Task(title = "new", remoteId = "uuid-new"), list)
        write()

        assertEquals(listOf("a", "new"), tree())
        val created = rows().first { it.title == "new" }
        assertTrue(caldavDao.getTask(created.id)!!.isMoved)
    }

    @Test
    fun aGoogleSubtaskWithNoPositionIsMovedRatherThanQuietlyNumbered() = runBlocking {
        useGoogleTasks()
        val a = newTask("a", parent = parent.id, order = 0)
        val b = newTask("b", parent = parent.id, order = null)
        stage()
        assertEquals(listOf("b", "a"), tree())

        trees.move(key = node("a").key, parentKey = rootKey, after = null)
        write()

        assertEquals(listOf("a", "b"), tree())
        assertEquals(1L, taskDao.fetch(b.id)!!.order)
        assertTrue(caldavDao.getTask(b.id)!!.isMoved)
        assertFalse(caldavDao.getTask(a.id)!!.isMoved)
    }

    @Test
    fun aRowCreatedSinceTheTreeLastRefreshedDoesNotRenumberItsSiblings() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = parent.id, order = 3)
        stage()
        val c = newTask("c", parent = parent.id, order = 2)

        val wrote = write()

        assertFalse(wrote)
        assertEquals(listOf("a", "c", "b"), tree())
        assertEquals(1L, taskDao.fetch(a.id)!!.order)
        assertEquals(2L, taskDao.fetch(c.id)!!.order)
        assertEquals(3L, taskDao.fetch(b.id)!!.order)
    }

    @Test
    fun aSubtaskIsNotCreatedTwiceWhenTheSaveRunsAgain() = runBlocking {
        stage()
        trees.add(rootKey, Task(title = "new", remoteId = "uuid-new"), list)

        write()
        write()

        assertEquals(listOf("new"), tree())
    }

    @Test
    fun aRenameCommittedElsewhereIsNotOverwrittenByAStagedOne() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        stage()
        val key = node("a").key
        trees.setTitle(key, "renamed here")

        taskSaver.save(taskDao.fetch(a.id)!!.copy(title = "renamed there"), null)
        trees.update(key) { it.copy(stagedTitle = null) }
        write()

        assertEquals("renamed there", taskDao.fetch(a.id)!!.title)
    }

    @Test
    fun aGoogleRunIsRenumberedAgainstWhereEachMoveLeftIt() = runBlocking {
        useGoogleTasks()
        newTask("a", parent = parent.id, order = 0)
        newTask("zulu", parent = parent.id, order = 1)
        newTask("alpha", parent = parent.id, order = 2)
        newTask("d", parent = parent.id, order = 3)
        stage()

        trees.move(key = node("a").key, parentKey = rootKey, after = node("d").key)
        write()

        assertEquals(listOf("zulu", "alpha", "d", "a"), tree())
        assertEquals(listOf(0L, 1L, 2L, 3L), rows().map { taskDao.fetch(it.id)!!.order })
    }

    @Test
    fun renamingAndTickingOffOnAGoogleListIsWrittenLikeAnywhereElse() = runBlocking {
        useGoogleTasks()
        val a = newTask("a", parent = parent.id, order = 0)
        val b = newTask("b", parent = parent.id, order = 1)
        stage()

        trees.setTitle(node("a").key, "renamed")
        trees.setCompleted(node("b").key, true)
        write()

        assertEquals("renamed", taskDao.fetch(a.id)!!.title)
        assertTrue(taskDao.fetch(b.id)!!.isCompleted)
    }

    @Test
    fun deletingOnAGoogleListLeavesItsSiblingsOrdinalsAlone() = runBlocking {
        useGoogleTasks()
        val a = newTask("a", parent = parent.id, order = 0)
        newTask("b", parent = parent.id, order = 1)
        val c = newTask("c", parent = parent.id, order = 2)
        stage()

        trees.delete(node("b").key)
        write()

        assertEquals(listOf("a", "c"), tree())
        assertEquals(0L, taskDao.fetch(a.id)!!.order)
        assertEquals(2L, taskDao.fetch(c.id)!!.order)
    }

    @Test
    fun aStagedTitleIsNotAppliedOverThePlaceholderTheSameSaveJustGaveIt() = runBlocking {
        stage()
        val outer = trees.add(rootKey, Task(title = "abc", remoteId = "uuid-outer"), list)
        trees.setTitle(outer.key, "")
        val inner = trees.add(rootKey, Task(title = "inner", remoteId = "uuid-inner"), list)
        trees.move(key = inner.key, parentKey = outer.key, after = null)

        write()

        assertEquals(listOf("(No title)", "  inner"), tree())
    }

    @Test
    fun anUntitledSubtaskHoldingOnlyAnotherUntitledOneIsNotCreated() = runBlocking {
        stage()
        val outer = trees.add(rootKey, Task(remoteId = "uuid-outer"), list)
        val inner = trees.add(rootKey, Task(remoteId = "uuid-inner"), list)
        trees.move(key = inner.key, parentKey = outer.key, after = null)

        write()

        assertEquals(emptyList<String>(), tree())
    }

    @Test
    fun reorderingARunOnlyPushesTheRowsThatActuallyMoved() = runBlocking {
        val rows = (0 until 6).map { newTask("t$it", parent = parent.id, order = it.toLong()) }
        stage()
        rows.forEach { dirtyDao.setDirtyState(caldavDao.getTask(it.id)!!.id, 1L, 1L) }

        trees.move(key = node("t1").key, parentKey = rootKey, after = null)
        write()

        assertEquals(listOf("t1", "t0", "t2", "t3", "t4", "t5"), tree())
        assertEquals(
            listOf("t0", "t1"),
            rows.filter { dirtyDao.isDirty(caldavDao.getTask(it.id)!!.id) == true }
                .mapNotNull { it.title }
                .sorted(),
        )
    }

    @Test
    fun rearrangingOneRunLeavesTheOthersAlone() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = parent.id, order = 2)
        val x = newTask("x", parent = a.id, order = 700_000_000L)
        val y = newTask("y", parent = a.id, order = 700_001_000L)
        val p = newTask("p", parent = b.id, order = 700_002_000L)
        val q = newTask("q", parent = b.id, order = 700_003_000L)
        stage()
        listOf(a, b, x, y, p, q).forEach {
            dirtyDao.setDirtyState(caldavDao.getTask(it.id)!!.id, 1L, 1L)
        }

        trees.move(key = node("y").key, parentKey = node("a").key, after = null)
        write()

        assertEquals(listOf("a", "  y", "  x", "b", "  p", "  q"), tree())
        assertEquals(
            listOf("x"),
            listOf(a, b, x, y, p, q)
                .filter { dirtyDao.isDirty(caldavDao.getTask(it.id)!!.id) == true }
                .mapNotNull { it.title },
        )
        assertEquals(700_002_000L, taskDao.fetch(p.id)!!.order)
        assertEquals(700_003_000L, taskDao.fetch(q.id)!!.order)
    }

    @Test
    fun reorderingSubtasksThatCameFromAServerKeepsTheirSortKeys() = runBlocking {
        val rows = (0 until 6).map {
            newTask("t$it", parent = parent.id, order = 700_000_000L + it * 1_000L)
        }
        stage()
        rows.forEach { dirtyDao.setDirtyState(caldavDao.getTask(it.id)!!.id, 1L, 1L) }

        trees.move(key = node("t4").key, parentKey = rootKey, after = node("t0").key)
        write()

        assertEquals(listOf("t0", "t4", "t1", "t2", "t3", "t5"), tree())
        assertEquals(
            listOf("t4"),
            rows.filter { dirtyDao.isDirty(caldavDao.getTask(it.id)!!.id) == true }
                .mapNotNull { it.title },
        )
        assertEquals(700_000_000L, taskDao.fetch(rows[0].id)!!.order)
        assertEquals(700_005_000L, taskDao.fetch(rows[5].id)!!.order)
    }

    @Test
    fun aSubtaskOnAMicrosoftListIsCreatedWithoutARemoteParent() = runBlocking {
        useAccountType(TYPE_MICROSOFT)
        newTask("a", parent = parent.id, order = 0)
        stage()

        trees.add(node("a").key, Task(title = "new", remoteId = "uuid-new"), list)
        write()

        val created = taskDao.fetch(rows().first { it.title == "new" }.id)!!
        assertEquals(parent.id, created.parent)
        assertNull(caldavDao.getTask(created.id)!!.remoteParent)
    }

    @Test
    fun positioningARunLeavesTheTreesOwnCopyOfTheRowAlone() = runBlocking {
        newTask("a", parent = parent.id, order = 1)
        newTask("b", parent = parent.id, order = 2)
        stage()
        val held = trees.get(node("b").key)!!.task

        trees.move(key = node("b").key, parentKey = rootKey, after = null)
        write()

        assertEquals(listOf("b", "a"), tree())
        assertEquals(2L, held.order)
        assertEquals(parent.id, held.parent)
    }

    @Test
    fun aBlankRowWithNothingInsideItIsNotFiledUnderThePlaceholder() = runBlocking {
        val existing = newTask("a", parent = parent.id, order = 1)
        stage()
        trees.add(rootKey, Task(remoteId = "uuid-blank"), list)
        trees.setTitle(node("a").key, "renamed")

        write()

        assertEquals(listOf("renamed"), tree())
        assertEquals("renamed", taskDao.fetch(existing.id)!!.title)
    }

    @Test
    fun everyBlankRowHoldingSubtasksIsFiledUnderThePlaceholder() = runBlocking {
        stage()
        listOf("one", "two").forEach { name ->
            val outer = trees.add(rootKey, Task(remoteId = "uuid-blank-$name"), list)
            val inner = trees.add(rootKey, Task(title = name, remoteId = "uuid-$name"), list)
            trees.indent(inner.key)
            assertEquals(rootKey, trees.get(outer.key)!!.parentKey)
        }

        write()

        assertEquals(listOf("(No title)", "  one", "(No title)", "  two"), tree())
    }

    @Test
    fun aBlankSubtaskIsNotKeptOnAListThatWillNotNestWhatIsInsideIt() = runBlocking {
        useGoogleTasks()
        stage()
        val outer = trees.add(rootKey, Task(remoteId = "uuid-blank"), list)
        val inner = trees.add(rootKey, Task(title = "inner", remoteId = "uuid-inner"), list)
        trees.indent(inner.key)

        write()

        assertEquals(listOf("inner"), tree())
    }

    @Test
    fun aFailureAfterTheCommitDoesNotSkipWhatWasStagedBehindIt() = runBlocking {
        val doomed = newTask("doomed", parent = parent.id, order = 1)
        stage()
        trees.add(rootKey, Task(title = "new", remoteId = "uuid-new"), list).let { node ->
            trees.update(node.key) {
                it.copy(
                    pending = it.pending!!.copy(
                        alarms = persistentSetOf(Alarm(time = 1_000L, type = TYPE_DATE_TIME))
                    )
                )
            }
        }
        trees.delete(node("doomed").key)
        whenever(alarmService.synchronizeAlarms(any(), any())).thenThrow(RuntimeException("no"))

        val failed = runCatching { write() }.exceptionOrNull()

        assertNotNull(failed)
        assertTrue(taskDao.fetch(doomed.id)!!.isDeleted)
        assertEquals(listOf("new"), tree())
    }

    @Test
    fun aRowCreatedByAFailedWriteIsNotCreatedTwiceByTheRetry() = runBlocking {
        stage()
        trees.add(rootKey, Task(title = "new", remoteId = "uuid-new"), list).let { node ->
            trees.update(node.key) {
                it.copy(
                    pending = it.pending!!.copy(
                        alarms = persistentSetOf(Alarm(time = 1_000L, type = TYPE_DATE_TIME))
                    )
                )
            }
        }
        whenever(alarmService.synchronizeAlarms(any(), any())).thenThrow(RuntimeException("no"))
        runCatching { write() }

        reset(alarmService)
        runCatching { write() }

        assertEquals(listOf("new"), tree())
    }

    @Test
    fun anUnpositionedGoogleRowIsSlottedInWithoutLandingOnASibling() = runBlocking {
        useGoogleTasks()
        val x = newTask("x", parent = parent.id, order = 0)
        val y = newTask("y", parent = parent.id, order = 1)
        val orphan = newTask("orphan", parent = parent.id, order = null)
        stage()

        trees.move(key = node("orphan").key, parentKey = rootKey, after = node("x").key)
        write()

        assertEquals(listOf("x", "orphan", "y"), tree())
        val orders = listOf(x, orphan, y).map { taskDao.fetch(it.id)!!.order }
        assertEquals(listOf(0L, 1L, 2L), orders)
    }

    @Test
    fun everythingStagedInOneEditorIsWrittenInOneSave() = runBlocking {
        val kept = newTask("kept", parent = parent.id, order = 1)
        val renamed = newTask("renamed", parent = parent.id, order = 2)
        val doomed = newTask("doomed", parent = parent.id, order = 3)
        stage()

        trees.add(rootKey, Task(title = "added", remoteId = "uuid-added"), list)
        trees.setTitle(node("renamed").key, "renamed here")
        trees.setCompleted(node("kept").key, true)
        trees.delete(node("doomed").key)
        trees.move(key = node("added").key, parentKey = rootKey, after = null)
        val wrote = write()

        assertTrue(wrote)
        assertEquals(listOf("added", "kept", "renamed here"), tree())
        assertTrue(taskDao.fetch(kept.id)!!.isCompleted)
        assertTrue(taskDao.fetch(doomed.id)!!.isDeleted)
        assertEquals("renamed here", taskDao.fetch(renamed.id)!!.title)
    }

    @Test
    fun aDeeplyNestedRunIsWrittenWithoutRunningOutOfStack() = runBlocking {
        stage()
        var previous: SubtaskNode? = null
        repeat(400) { depth ->
            val added = trees.add(rootKey, Task(title = "d$depth", remoteId = "uuid-d$depth"), list)
            if (previous != null) {
                trees.move(key = added.key, parentKey = previous!!.key, after = null)
            }
            previous = trees.get(added.key)
        }

        write()

        assertEquals(400, rows().size)
        assertEquals(399, rows().maxOf { it.indent })
    }

    @Test
    fun anUntitledSubtaskThatCarriesSomethingElseIsKeptUnderAPlaceholder() = runBlocking {
        stage()
        trees.add(
            rootKey,
            Task(remoteId = "uuid-dated", dueDate = DUE_AT, notes = "buy the good one"),
            list,
        )

        write()

        assertEquals(listOf("(No title)"), tree())
        val created = taskDao.fetch(rows().single().id)!!
        assertEquals(DUE_AT, created.dueDate)
        assertEquals("buy the good one", created.notes)
    }

    private suspend fun untitledRowCarrying(
        task: Task = Task(remoteId = "uuid-blank"),
        pending: (PendingTask) -> PendingTask = { it },
        stage: (SubtaskNode) -> Unit = {},
    ): List<String> {
        stage()
        val added = trees.add(rootKey, task, list)
        trees.update(added.key) { it.copy(pending = pending(it.pending!!)) }
        stage(added)

        write()

        return tree()
    }

    @Test
    fun anUntitledRowWithADueDateIsKept() = runBlocking {
        assertEquals(
            listOf("(No title)"),
            untitledRowCarrying(Task(remoteId = "uuid-blank", dueDate = DUE_AT)),
        )
    }

    @Test
    fun anUntitledRowWithADescriptionIsKept() = runBlocking {
        assertEquals(
            listOf("(No title)"),
            untitledRowCarrying(Task(remoteId = "uuid-blank", notes = "buy the good one")),
        )
    }

    @Test
    fun anUntitledRowWithAStartDateIsKept() = runBlocking {
        assertEquals(
            listOf("(No title)"),
            untitledRowCarrying(Task(remoteId = "uuid-blank", hideUntil = DUE_AT)),
        )
    }

    @Test
    fun anUntitledRowWithAPriorityIsKept() = runBlocking {
        assertEquals(
            listOf("(No title)"),
            untitledRowCarrying(Task(remoteId = "uuid-blank", priority = Task.Priority.HIGH)),
        )
    }

    @Test
    fun anUntitledRowWithARepeatRuleIsKept() = runBlocking {
        assertEquals(
            listOf("(No title)"),
            untitledRowCarrying(Task(remoteId = "uuid-blank", recurrence = "RRULE:FREQ=DAILY")),
        )
    }

    @Test
    fun anUntitledRowWithAnEstimateIsKept() = runBlocking {
        assertEquals(
            listOf("(No title)"),
            untitledRowCarrying(Task(remoteId = "uuid-blank", estimatedSeconds = 600)),
        )
    }

    @Test
    fun anUntitledRowWithTimeAlreadyLoggedAgainstItIsKept() = runBlocking {
        assertEquals(
            listOf("(No title)"),
            untitledRowCarrying(Task(remoteId = "uuid-blank", elapsedSeconds = 600)),
        )
    }

    @Test
    fun anUntitledRowWithTagsIsKept() = runBlocking {
        assertEquals(
            listOf("(No title)"),
            untitledRowCarrying(pending = { it.copy(tags = listOf(TagData(name = "work"))) }),
        )
    }

    @Test
    fun anUntitledRowWithAStartSelectionIsKept() = runBlocking {
        assertEquals(
            listOf("(No title)"),
            untitledRowCarrying(pending = { it.copy(startDay = DUE_DATE) }),
        )
    }

    @Test
    fun anUntitledRowTickedOffIsKept() = runBlocking {
        assertEquals(
            listOf("(No title)"),
            untitledRowCarrying(stage = { trees.setCompleted(it.key, completed = true) }),
        )
    }

    @Test
    fun anEmptyRowWithNothingOnItIsStillDropped() = runBlocking {
        stage()
        trees.add(rootKey, Task(remoteId = "uuid-blank"), list)

        write()

        assertEquals(emptyList<String>(), tree())
    }

    @Test
    fun anEmptyRowCarryingOnlyTheDefaultRemindersIsStillDropped() = runBlocking {
        stage()
        val blank = trees.add(rootKey, Task(remoteId = "uuid-blank"), list)
        trees.update(blank.key) {
            it.copy(pending = it.pending!!.copy(alarms = persistentSetOf(Alarm(time = 1_000L, type = TYPE_DATE_TIME))))
        }

        write()

        assertEquals(emptyList<String>(), tree())
    }

    @Test
    fun aBlankRowTheMergeRescuesIsStillGivenAPlaceholder() = runBlocking {
        val doomed = newTask("doomed", parent = parent.id, order = 1)
        val dragged = newTask("dragged", parent = parent.id, order = 2)
        stage()
        val blank = trees.add(rootKey, Task(remoteId = "uuid-blank"), list)
        trees.move(key = blank.key, parentKey = node("doomed").key, after = null)
        trees.move(key = node("dragged").key, parentKey = blank.key, after = null)
        trees.delete(node("doomed").key)
        assertEquals(
            setOf(node("doomed").key, blank.key, node("dragged").key),
            trees.rowsOf(rootKey).doomed(),
        )
        taskDeleter.markDeleted(listOf(doomed.id))

        write()

        assertEquals(listOf("(No title)", "  dragged"), tree())
        assertEquals(
            rows().first { it.title == "(No title)" }.id,
            taskDao.fetch(dragged.id)!!.parent,
        )
    }

    @Test
    fun deletingASubtaskTakesItsNestedRowsWithItOnAnyList() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        val b = newTask("b", parent = a.id, order = 2)
        useAccountType(TYPE_MICROSOFT)
        stage()
        assertEquals(listOf("a", "  b"), trees.rowsOf(rootKey).map { "  ".repeat(it.indent) + it.node.title })

        trees.delete(node("a").key)
        write()

        assertTrue(taskDao.fetch(a.id)!!.isDeleted)
        assertTrue(taskDao.fetch(b.id)!!.isDeleted)
    }

    @Test
    fun theSecondSaveInAStackFindsTheRowTheFirstOneMade() = runBlocking {
        val a = newTask("a", parent = parent.id, order = 1)
        stage()
        val added = trees.add(subtaskKey(a), Task(title = "new", remoteId = "uuid-new"), list)

        write()
        writer.write(trees, subtaskKey(a), a, list, untitled = "(No title)")

        assertEquals(listOf("a", "  new"), tree())
        assertEquals(1, rows().count { it.title == "new" })
    }

    @Test
    fun aTickThatCouldNotBeAppliedIsLeftForTheNextSave() = runBlocking {
        stage()
        val added = trees.add(rootKey, Task(title = "new", remoteId = "uuid-new"), list)
        trees.setCompleted(added.key, true)
        val completer: TaskCompleter = mock()
        whenever(completer.setComplete(any<Task>(), any(), any()))
            .thenThrow(RuntimeException("repeat handling blew up"))

        val failed = runCatching {
            writerWith(completer).write(trees, rootKey, parent, list, untitled = "(No title)")
        }.exceptionOrNull()

        assertNotNull(failed)
        assertFalse(taskDao.fetch(trees.get(added.key)!!.id)!!.isCompleted)
        assertTrue(trees.get(added.key)!!.completionEdited)
        assertTrue(trees.isRearranged(rootKey))

        write()
        assertTrue(taskDao.fetch(trees.get(added.key)?.id ?: rows().first().id)!!.isCompleted)
    }

    @Test
    fun remindersThatLandedAfterTheRowWasCreatedAreStillScheduled() = runBlocking {
        stage()
        val added = trees.add(rootKey, Task(title = "new", remoteId = "uuid-new"), list)
        write()
        reset(alarmService)
        whenever(alarmService.synchronizeAlarms(any(), any())).thenAnswer { true }
        val alarm = Alarm(time = 1_000L, type = TYPE_DATE_TIME)
        trees.update(added.key) { it.copy(pending = PendingTask(list = list, alarms = persistentSetOf(alarm))) }

        assertTrue(trees.isRearranged(rootKey))
        write()

        verify(alarmService).synchronizeAlarms(any(), check { assertTrue(it.isNotEmpty()) })
        assertNull(trees.get(added.key)?.pending)
        assertFalse(trees.isRearranged(rootKey))
    }

    companion object {
        private const val ACCOUNT = "account-1"
        private const val CALENDAR = "calendar-1"
        private const val COMPLETED_AT = 1_700_000_000_000L
        private const val DUE_AT = 1_700_000_000_000L
    }
}
