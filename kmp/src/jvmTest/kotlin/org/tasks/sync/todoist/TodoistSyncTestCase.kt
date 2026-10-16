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
abstract class TodoistSyncTestCase : DatabaseTest() {
    protected val taskDao = db.taskDao()
    protected val caldavDao = db.caldavDao()
    protected val dirtyDao = db.dirtyDao()

    protected val taskDeleter = TaskDeleter(
        deletionDao = db.deletionDao(),
        taskDao = taskDao,
        caldavDao = caldavDao,
        refreshBroadcaster = mock(),
        vtodoCache = mock(),
        tasksPreferences = mock(),
        taskCleanup = object : org.tasks.service.TaskCleanup {},
    )

    protected val taskSaver = TaskSaver(
        taskDao = taskDao,
        refreshBroadcaster = mock(),
        notifier = mock(),
        locationService = mock(),
        timerPlugin = mock(),
        backgroundWork = mock(),
        caldavDao = caldavDao,
    )

    protected lateinit var service: FakeTodoistService
    protected lateinit var synchronizer: TodoistSynchronizer
    protected lateinit var account: CaldavAccount

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
        val stores = TodoistSyncStores(
            caldavDao = caldavDao,
            taskDao = taskDao,
            dirtyDao = dirtyDao,
            tagDao = db.tagDao(),
            tagDataDao = db.tagDataDao(),
        )
        val services = TodoistSyncServices(
            taskSaver = taskSaver,
            taskDeleter = taskDeleter,
            refreshBroadcaster = mock(),
            reporting = mock(),
        )
        val hooks = TodoistSyncHooks(
            vtodoCache = mock(),
            createTask = { Task() },
            setDefaultList = { },
        )
        synchronizer = TodoistSynchronizer(
            clientProvider = object : TodoistClientProvider {
                override suspend fun getService(account: CaldavAccount) = service.asService()
                override suspend fun getService(token: String) = service.asService()
                override suspend fun hasCredentials(account: CaldavAccount) = true
            },
            stores = stores,
            services = services,
            hooks = hooks,
            pushCommands = TodoistPushCommands(stores, services, hooks),
            pushResults = TodoistPushResults(stores, services, hooks),
        )
    }

    protected suspend fun reloadAccount() {
        account = caldavDao.getAccount(account.id)!!
    }

    companion object {
        val UUID_REGEX =
            Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

        const val ACCOUNT = "todoist-account"
        const val TEMP = "temp"
        const val COMPLETED_AT = "2026-10-01T12:00:00Z"

        fun project(id: String, name: String, inbox: Boolean = false) =
            TodoistProject(id = id, name = name, isInboxProject = inbox)

    }
}
