package org.tasks.mcp

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.data.UUIDHelper
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task

class DatabaseTasksApiTest : McpGraphTestCase() {

    private lateinit var listUuid: String

    @Before
    fun seedList() = runBlocking {
        listUuid = UUIDHelper.newUUID()
        db.caldavDao().insert(
            CaldavAccount(uuid = "account", name = "Local", accountType = CaldavAccount.TYPE_LOCAL)
        )
        db.caldavDao().insert(
            CaldavCalendar(uuid = listUuid, account = "account", name = "Inbox")
        )
    }

    @Test
    fun rendersATimestampAsMillisAndLocalIso() = runBlocking {
        val id = addTask(title = "Pay rent", dueDate = MINUTE_ALIGNED)

        val task = api.getTask(id)!!.toApiTask()

        assertEquals(MINUTE_ALIGNED, task.due?.millis)
        assertTrue("expected an ISO string, got ${task.due?.iso}", task.due?.iso?.contains('T') == true)
    }

    @Test
    fun writingFromInsideTheTransactionDoesNotDeadlockAcrossDispatchers() = runBlocking {
        val id = withTimeout(10_000) {
            db.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    withContext(Dispatchers.IO) {
                        val taskId = db.taskDao().insert(
                            Task(title = "Nested", remoteId = UUIDHelper.newUUID())
                        )
                        db.caldavDao().insert(CaldavTask(task = taskId, calendar = listUuid))
                        taskId
                    }
                }
            }
        }

        assertEquals("Nested", api.getTask(id)?.title)
    }

    private companion object {
        const val MINUTE_ALIGNED = 1_756_999_980_000L
    }

    private suspend fun addTask(title: String, dueDate: Long = 0): Long {
        val task = Task(title = title, dueDate = dueDate, remoteId = UUIDHelper.newUUID())
        val id = db.taskDao().insert(task)
        db.caldavDao().insert(CaldavTask(task = id, calendar = listUuid))
        return id
    }
}
