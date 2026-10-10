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
class TodoistSynchronizerTest : DatabaseTest() {
    private val taskDao = db.taskDao()
    private val caldavDao = db.caldavDao()
    private val dirtyDao = db.dirtyDao()

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

    private lateinit var service: FakeTodoistService
    private lateinit var synchronizer: TodoistSynchronizer
    private lateinit var account: CaldavAccount

    @Before
    fun setUp() = runBlocking {
        service = FakeTodoistService()
        account = CaldavAccount(
            uuid = ACCOUNT,
            accountType = CaldavAccount.TYPE_TODOIST,
            password = "encrypted",
        )
        val id = caldavDao.insert(account)
        account = caldavDao.getAccount(id)!!
        synchronizer = TodoistSynchronizer(
            caldavDao = caldavDao,
            taskDao = taskDao,
            dirtyDao = dirtyDao,
            taskSaver = taskSaver,
            refreshBroadcaster = mock(),
            taskDeleter = taskDeleter,
            reporting = mock(),
            clientProvider = object : TodoistClientProvider {
                override suspend fun getService(account: CaldavAccount) = service.asService()
                override suspend fun getService(token: String) = service.asService()
                override suspend fun hasCredentials(account: CaldavAccount) = true
            },
            tagDao = db.tagDao(),
            tagDataDao = db.tagDataDao(),
            vtodoCache = mock(),
            createTask = { Task() },
            setDefaultList = { },
        )
    }

    private suspend fun reloadAccount() {
        account = caldavDao.getAccount(account.id)!!
    }

    @Test
    fun createsListsAndTasksOnFullSync() = runBlocking {
        service.enqueue(
            syncToken = "token-1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(item("i1", "Buy milk", projectId = "p1", priority = 4)),
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
        service.enqueue(syncToken = "token-1", fullSync = true)
        synchronizer.sync(account)
        reloadAccount()
        assertEquals("token-1", account.syncToken)

        service.enqueue(syncToken = "token-2")
        synchronizer.sync(account)
        // The second request must carry the token from the first response.
        assertEquals(listOf("*", "token-1"), service.requestedTokens)
        reloadAccount()
        assertEquals("token-2", account.syncToken)
    }

    @Test
    fun deltaAbsenceDoesNotDeleteTasks() = runBlocking {
        service.enqueue(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(item("i1", "Keep me", projectId = "p1")),
        )
        synchronizer.sync(account)
        val taskId = caldavDao.getTaskByRemoteId("p1", "i1")!!.task

        // A delta that mentions nothing at all means "nothing changed".
        service.enqueue(syncToken = "t2")
        synchronizer.sync(account)

        assertNotNull(taskDao.fetch(taskId))
    }

    @Test
    fun deletesTaskWhenDeltaSaysIsDeleted() = runBlocking {
        service.enqueue(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(item("i1", "Delete me", projectId = "p1")),
        )
        synchronizer.sync(account)
        val taskId = caldavDao.getTaskByRemoteId("p1", "i1")!!.task

        service.enqueue(
            syncToken = "t2",
            items = listOf(item("i1", "Delete me", projectId = "p1", isDeleted = true)),
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
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(item("i1", "Finish me", projectId = "p1")),
        )
        synchronizer.sync(account)
        val caldavTask = caldavDao.getTaskByRemoteId("p1", "i1")!!
        val task = taskDao.fetch(caldavTask.task)!!

        // Complete it remotely: it vanishes from items but is still there.
        service.enqueue(
            syncToken = "t2",
            items = listOf(item("i1", "Finish me", projectId = "p1", checked = true)),
        )
        synchronizer.sync(account)
        assertTrue(taskDao.fetch(task.id)!!.isCompleted)

        // Now a fresh full sync with the task absent, as the real API would answer.
        service.enqueue(
            syncToken = "t3",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = emptyList(),
        )
        synchronizer.sync(account)

        assertNotNull("completed task must survive a full sync", taskDao.fetch(task.id))
    }

    @Test
    fun fullSyncReapsIncompleteTasksThatWentAway() = runBlocking {
        service.enqueue(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(item("i1", "Gone", projectId = "p1")),
        )
        synchronizer.sync(account)
        val taskId = caldavDao.getTaskByRemoteId("p1", "i1")!!.task

        service.enqueue(
            syncToken = "t2",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = emptyList(),
        )
        synchronizer.sync(account)

        assertTrue(taskDao.fetch(taskId)?.isDeleted != false)
    }

    @Test
    fun resolvesParentHierarchyFromRemoteParentIds() = runBlocking {
        // The child arrives before the parent, which the DAO parent fix-up must handle.
        service.enqueue(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(
                item("i2", "Child", projectId = "p1", parentId = "i1"),
                item("i1", "Parent", projectId = "p1"),
            ),
        )
        synchronizer.sync(account)

        val parent = caldavDao.getTaskByRemoteId("p1", "i1")!!
        val child = caldavDao.getTaskByRemoteId("p1", "i2")!!
        assertEquals("i1", child.remoteParent)
        assertEquals(parent.task, taskDao.fetch(child.task)!!.parent)
    }

    @Test
    fun pushesNewLocalTaskAndRecordsAssignedId() = runBlocking {
        service.enqueue(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
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

        service.enqueue(syncToken = "t2")
        service.enqueue(syncToken = "t3", tempIdMapping = mapOf(TEMP to "server-1"))
        synchronizer.sync(account)

        val command = service.commands.flatten().single { it.type == TodoistCommand.ITEM_ADD }
        assertEquals("Local task", command.args["content"]!!.jsonPrimitive.content)
        assertEquals("p1", command.args["project_id"]!!.jsonPrimitive.content)

        val caldavTask = caldavDao.getTask(task.id)!!
        assertEquals("server-1", caldavTask.remoteId)
        assertFalse(dirtyDao.isDirty(caldavTask.id) == true)
    }

    @Test
    fun pushesCompletionAsASeparateCommand() = runBlocking {
        service.enqueue(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
            items = listOf(item("i1", "Complete me", projectId = "p1")),
        )
        synchronizer.sync(account)

        val caldavTask = caldavDao.getTaskByRemoteId("p1", "i1")!!
        val task = taskDao.fetch(caldavTask.task)!!
        val original = task.copy()
        task.completionDate = 1_700_000_000_000L
        taskSaver.save(task, original, dirty = true)

        service.enqueue(syncToken = "t2")
        service.enqueue(syncToken = "t3")
        synchronizer.sync(account)

        val types = service.commands.flatten().map { it.type }
        assertTrue(
            "expected an item_complete, got $types",
            TodoistCommand.ITEM_COMPLETE in types,
        )
    }

    @Test
    fun commandIdentifiersAreRealUuids() = runBlocking {
        // Todoist rejects a command whose uuid is not a UUID. UUIDHelper.newUUID()
        // returns an Astrid-legacy decimal id, not a UUID, so it must not be used here.
        service.enqueue(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
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

        service.enqueue(syncToken = "t2")
        service.enqueue(syncToken = "t3", tempIdMapping = mapOf(TEMP to "server-1"))
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
    }

    @Test
    fun rejectedPushSurfacesOnTheAccount() = runBlocking {
        // A command the server refuses must not look like a successful sync, or it
        // retries forever with nothing in the UI explaining the task never went up.
        service.enqueue(
            syncToken = "t1",
            fullSync = true,
            projects = listOf(project("p1", "Work")),
        )
        synchronizer.sync(account)

        val task = Task(title = "Local task")
        taskDao.createNew(task)
        caldavDao.insert(
            task = task,
            caldavTask = org.tasks.data.entity.CaldavTask(task = task.id, calendar = "p1"),
            addToTop = false,
        )

        service.enqueue(syncToken = "t2")
        service.enqueue(
            syncToken = "t3",
            okAll = false,
            errors = mapOf(TodoistCommand.ITEM_ADD to (15 to "Invalid temp id")),
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

    companion object {
        private val UUID_REGEX =
            Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

        private const val ACCOUNT = "todoist-account"
        private const val TEMP = "temp"

        private fun project(id: String, name: String, inbox: Boolean = false) =
            TodoistProject(id = id, name = name, isInboxProject = inbox)

        private fun item(
            id: String,
            content: String,
            projectId: String? = null,
            parentId: String? = null,
            priority: Int = 1,
            checked: Boolean = false,
            isDeleted: Boolean = false,
        ) = TodoistItem(
            id = id,
            content = content,
            projectId = projectId,
            parentId = parentId,
            priority = priority,
            checked = checked,
            isDeleted = isDeleted,
            completedAt = if (checked) "2026-10-01T12:00:00Z" else null,
        )
    }
}
