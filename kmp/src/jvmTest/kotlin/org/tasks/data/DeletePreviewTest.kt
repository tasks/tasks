package org.tasks.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.tasks.DatabaseTest
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.preferences.AppPreferences

class DeletePreviewTest : DatabaseTest() {
    private val taskDao = db.taskDao()

    @Before
    fun setUp() = runBlocking {
        caldavDao.insert(CaldavAccount(uuid = ACCOUNT, accountType = TYPE_CALDAV))
        caldavDao.insert(CaldavCalendar(uuid = LIST, account = ACCOUNT))
    }

    private val caldavDao get() = db.caldavDao()

    private suspend fun newTask(title: String, parent: Long = 0): Task {
        val task = Task(title = title, parent = parent, remoteId = "uuid-$title")
        taskDao.createNew(task)
        caldavDao.insert(
            task = task,
            caldavTask = CaldavTask(task = task.id, calendar = LIST, remoteId = "uuid-$title"),
            addToTop = false,
        )
        return task
    }

    @Test
    fun thePreviewIsTheWholeSubtreeInBreadthFirstOrder() = runBlocking {
        val root = newTask("root")
        val child = newTask("child", parent = root.id)
        val grandchild = newTask("grandchild", parent = child.id)
        newTask("unrelated")

        val preview = DeletePreview.forTask(taskDao, root.id)

        assertEquals(listOf("root", "child", "grandchild"), preview.map { it.task.title })
        assertEquals(listOf(0, 1, 2), preview.map { it.depth })
        assertTrue("nothing here is read-only", preview.none { it.readOnly })
    }

    @Test
    fun readOnlyDescendantsAreFlaggedButStillListed() = runBlocking {
        val root = newTask("root")
        val shared = newTask("shared", parent = root.id).apply {
            readOnly = true
            taskDao.update(this, null)
        }

        val preview = DeletePreview.forTask(taskDao, root.id)

        assertEquals(listOf(false, true), preview.map { it.readOnly })
        assertEquals(listOf("root", "shared"), preview.map { it.task.title })
    }

    @Test
    fun aReadOnlyRootIsFlaggedToo() = runBlocking {
        val root = newTask("shared").apply {
            readOnly = true
            taskDao.update(this, null)
        }
        newTask("child", parent = root.id)

        val preview = DeletePreview.forTask(taskDao, root.id)

        assertTrue("the root itself is read-only", preview.first().readOnly)
        assertEquals(2, preview.size)
    }

    @Test
    fun deletedDescendantsAreSkippedLikeTheDeleteItself() = runBlocking {
        val root = newTask("root")
        val child = newTask("child", parent = root.id)
        val doomed = newTask("doomed", parent = child.id)
        doomed.deletionDate = 1
        taskDao.update(doomed, null)

        val preview = DeletePreview.forTask(taskDao, root.id)

        assertEquals(listOf("root", "child"), preview.map { it.task.title })
    }

    @Test
    fun aLeafTaskPreviewsItselfAlone() = runBlocking {
        val solo = newTask("solo")
        newTask("other")

        val preview = DeletePreview.forTask(taskDao, solo.id)

        assertEquals(listOf("solo"), preview.map { it.task.title })
    }

    companion object {
        private const val ACCOUNT = "account"
        private const val LIST = "list"
    }
}
