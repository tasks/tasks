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
class TodoistSyncPushTest : TodoistSyncTestCase() {    @Test
    fun pushesNewLocalTaskAndRecordsAssignedId() = runBlocking {
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            )
        )
        synchronizer.sync(account)

        val task = Task(title = "Local task")
        taskDao.createNew(task)
        caldavDao.insert(
            task = task,
            caldavTask = org.tasks.data.entity.CaldavTask(
                task = task.id,
                calendar = "p1",
                remoteId = null,
            ),
            addToTop = false,
        )

        service.enqueue("t2")
        service.enqueue(
            FakeTodoistService.Reply(syncToken = "t3", tempIdMapping = mapOf(TEMP to "server-1")
            )
        )
        synchronizer.sync(account)

        val command = service.commands.flatten().single { it.type == TodoistCommand.ITEM_ADD }
        assertEquals("Local task", command.args["content"]!!.jsonPrimitive.content)
        assertEquals("p1", command.args["project_id"]!!.jsonPrimitive.content)

        val caldavTask = caldavDao.getTask(task.id)!!
        assertEquals("server-1", caldavTask.remoteId)
        assertFalse(dirtyDao.isDirty(caldavTask.id) == true)
    }    @Test
    fun pushesCompletionAsASeparateCommand() = runBlocking {
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(TodoistItem("i1", "Complete me", projectId = "p1")),
            )
        )
        synchronizer.sync(account)

        val caldavTask = caldavDao.getTaskByRemoteId("p1", "i1")!!
        val task = taskDao.fetch(caldavTask.task)!!
        val original = task.copy()
        task.completionDate = 1_700_000_000_000L
        taskSaver.save(task, original, dirty = true)

        service.enqueue("t2")
        service.enqueue("t3")
        synchronizer.sync(account)

        val types = service.commands.flatten().map { it.type }
        assertTrue(
            "expected an item_complete, got $types",
            TodoistCommand.ITEM_COMPLETE in types,
        )
    }    @Test
    fun commandIdentifiersAreRealUuids() = runBlocking {
        // Todoist rejects a command whose uuid is not a UUID. UUIDHelper.newUUID()
        // returns an Astrid-legacy decimal id, not a UUID, so it must not be used here.
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            )
        )
        synchronizer.sync(account)

        val parent = Task(title = "Parent")
        taskDao.createNew(parent)
        caldavDao.insert(
            task = parent,
            caldavTask = org.tasks.data.entity.CaldavTask(task = parent.id, calendar = "p1"),
            addToTop = false,
        )
        val child = Task(title = "Child", parent = parent.id)
        taskDao.createNew(child)
        caldavDao.insert(
            task = child,
            caldavTask = org.tasks.data.entity.CaldavTask(task = child.id, calendar = "p1"),
            addToTop = false,
        )

        service.enqueue("t2")
        service.enqueue(
            FakeTodoistService.Reply(syncToken = "t3", tempIdMapping = mapOf(TEMP to "server-1")
            )
        )
        synchronizer.sync(account)

        val sent = service.commands.flatten()
        assertTrue("expected commands to be sent, got none", sent.isNotEmpty())
        sent.forEach { command ->
            assertTrue(
                "command uuid '${command.uuid}' is not a UUID (type=${command.type})",
                UUID_REGEX.matches(command.uuid),
            )
            command.tempId?.let {
                assertTrue("temp_id '$it' is not a UUID", UUID_REGEX.matches(it))
            }
        }
    }    @Test
    fun rejectedPushSurfacesOnTheAccount() = runBlocking {
        // A command the server refuses must not look like a successful sync, or it
        // retries forever with nothing in the UI explaining the task never went up.
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            )
        )
        synchronizer.sync(account)

        val task = Task(title = "Local task")
        taskDao.createNew(task)
        caldavDao.insert(
            task = task,
            caldavTask = org.tasks.data.entity.CaldavTask(task = task.id, calendar = "p1"),
            addToTop = false,
        )

        service.enqueue("t2")
        service.enqueue(
            FakeTodoistService.Reply(
            syncToken = "t3",
            okAll = false,
            errors = mapOf(TodoistCommand.ITEM_ADD to (15 to "Invalid temp id")),
            )
        )
        synchronizer.sync(account)

        reloadAccount()
        val error = account.error
        assertTrue("expected an account error, got '$error'", !error.isNullOrBlank())
        assertTrue("error should name the failure, got '$error'", error!!.contains("Invalid temp id"))
        // Still dirty, so a later fix retries rather than losing the task.
        val caldavTask = caldavDao.getTask(task.id)!!
        assertTrue(dirtyDao.isDirty(caldavTask.id) == true)
    }
}
