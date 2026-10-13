package org.tasks.sync.todoist

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.DatabaseTest
import org.tasks.data.TaskSaver
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Task
import org.tasks.service.TaskDeleter

/**
 * End-to-end tests for [TodoistSynchronizer] against the real DAOs and a scripted
 * service, covering the delta semantics that are easy to get wrong: absence meaning
 * "unchanged", completed tasks never appearing in `items`, and temp-id resolution.
 */
class TodoistSyncPullTest : TodoistSyncTestCase() {
    @Test
    fun createsListsAndTasksOnFullSync() = runBlocking {
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "token-1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(TodoistItem("i1", "Buy milk", projectId = "p1", priority = 4)),
            )
        )
        synchronizer.sync(account)

        val lists = caldavDao.getCalendarsByAccount(ACCOUNT)
        assertEquals(1, lists.size)
        assertEquals("Work", lists.single().name)

        val caldavTask = caldavDao.getTaskByRemoteId("p1", "i1")
        assertNotNull(caldavTask)
        val task = taskDao.fetch(caldavTask!!.task)!!
        assertEquals("Buy milk", task.title)
        assertEquals(Task.Priority.HIGH, task.priority)
    }

    @Test
    fun persistsSyncTokenAndReplaysItOnTheNextSync() = runBlocking {
        service.enqueue(
            FakeTodoistService.Reply(syncToken = "token-1", fullSync = true
            )
        )
        synchronizer.sync(account)
        reloadAccount()
        assertEquals("token-1", account.syncToken)

        service.enqueue("token-2")
        synchronizer.sync(account)
        // The second request must carry the token from the first response.
        assertEquals(listOf("*", "token-1"), service.requestedTokens)
        reloadAccount()
        assertEquals("token-2", account.syncToken)
    }

    @Test
    fun deltaAbsenceDoesNotDeleteTasks() = runBlocking {
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(TodoistItem("i1", "Keep me", projectId = "p1")),
            )
        )
        synchronizer.sync(account)
        val taskId = caldavDao.getTaskByRemoteId("p1", "i1")!!.task

        // A delta that mentions nothing at all means "nothing changed".
        service.enqueue("t2")
        synchronizer.sync(account)

        assertNotNull(taskDao.fetch(taskId))
    }

    @Test
    fun deletesTaskWhenDeltaSaysIsDeleted() = runBlocking {
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(TodoistItem("i1", "Delete me", projectId = "p1")),
            )
        )
        synchronizer.sync(account)
        val taskId = caldavDao.getTaskByRemoteId("p1", "i1")!!.task

        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t2",
            items = listOf(TodoistItem("i1", "Delete me", projectId = "p1", isDeleted = true)),
            )
        )
        synchronizer.sync(account)

        assertNull(caldavDao.getTaskByRemoteId("p1", "i1"))
        assertTrue(taskDao.fetch(taskId)?.isDeleted != false)
    }

    @Test
    fun fullSyncDoesNotReapCompletedTasks() = runBlocking {
        // Completed tasks are excluded from the API's `items`, so a full sync that
        // omits one must not be read as a deletion.
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(TodoistItem("i1", "Finish me", projectId = "p1")),
            )
        )
        synchronizer.sync(account)
        val caldavTask = caldavDao.getTaskByRemoteId("p1", "i1")!!
        val task = taskDao.fetch(caldavTask.task)!!

        // Complete it remotely: it vanishes from items but is still there.
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t2",
            items = listOf(TodoistItem("i1", "Finish me", projectId = "p1", checked = true, completedAt = COMPLETED_AT)),
            )
        )
        synchronizer.sync(account)
        assertTrue(taskDao.fetch(task.id)!!.isCompleted)

        // Now a fresh full sync with the task absent, as the real API would answer.
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t3",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = emptyList(),
            )
        )
        synchronizer.sync(account)

        assertNotNull("completed task must survive a full sync", taskDao.fetch(task.id))
    }

    @Test
    fun fullSyncReapsIncompleteTasksThatWentAway() = runBlocking {
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(TodoistItem("i1", "Gone", projectId = "p1")),
            )
        )
        synchronizer.sync(account)
        val taskId = caldavDao.getTaskByRemoteId("p1", "i1")!!.task

        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t2",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = emptyList(),
            )
        )
        synchronizer.sync(account)

        assertTrue(taskDao.fetch(taskId)?.isDeleted != false)
    }

    @Test
    fun resolvesParentHierarchyFromRemoteParentIds() = runBlocking {
        // The child arrives before the parent, which the DAO parent fix-up must handle.
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(
                TodoistItem("i2", "Child", projectId = "p1", parentId = "i1"),
                TodoistItem("i1", "Parent", projectId = "p1"),
            ),
            )
        )
        synchronizer.sync(account)

        val parent = caldavDao.getTaskByRemoteId("p1", "i1")!!
        val child = caldavDao.getTaskByRemoteId("p1", "i2")!!
        assertEquals("i1", child.remoteParent)
        assertEquals(parent.task, taskDao.fetch(child.task)!!.parent)
    }
}
