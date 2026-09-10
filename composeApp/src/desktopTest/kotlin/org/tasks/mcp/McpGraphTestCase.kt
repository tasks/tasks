package org.tasks.mcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.tasks.api.TaskWrite
import org.tasks.data.db.Database
import org.tasks.di.commonModule
import org.tasks.di.platformModule
import org.tasks.di.resetDirectories
import org.tasks.extensions.closeQuietly

abstract class McpGraphTestCase {
    private var koinInstance: Koin? = null
    private var previousDataDir: String? = null

    protected val koin: Koin get() = checkNotNull(koinInstance) { "graph not started" }

    protected val api: DatabaseTasksApi get() = koin.get()

    protected val db: Database get() = koin.get()

    @Before
    fun startGraph() {
        previousDataDir = System.getProperty(DATA_DIR)
        System.setProperty(DATA_DIR, createTempDirectory().absolutePath)
        resetDirectories()
        koinInstance = startKoin { modules(commonModule, platformModule()) }.koin
    }

    @After
    fun stopGraph() {
        koinInstance?.let { koin ->
            closeQuietly(TAG, "the MCP server") {
                kotlinx.coroutines.runBlocking {
                    koin.get<DesktopMcpServerController>().shutdown()
                }
            }
            closeQuietly(TAG, "the database") { koin.get<Database>().close() }
            closeQuietly(TAG, "the scope") { koin.get<CoroutineScope>().cancel() }
        }
        try {
            stopKoin()
        } finally {
            koinInstance = null
            previousDataDir
                ?.let { System.setProperty(DATA_DIR, it) }
                ?: System.clearProperty(DATA_DIR)
            resetDirectories()
        }
    }

    private suspend fun seed() {
        if (api.lists().isEmpty()) {
            api.deleteTask(api.createTask(title = "seed"))
        }
    }

    protected suspend fun defaultListId(): Long {
        seed()
        return api.lists().first().id
    }

    protected suspend fun accountId(): Long {
        seed()
        return api.accounts().first().id
    }

    protected suspend fun DatabaseTasksApi.createTask(
        title: String,
        notes: String? = null,
        dueDate: Long? = null,
        dueAllDay: Boolean? = null,
    ): Long = createTasks(
        listOf(TaskWrite(title = title, notes = notes, due = dueDate, dueAllDay = dueAllDay))
    ).ids.single()

    private fun createTempDirectory() =
        java.nio.file.Files.createTempDirectory("tasks-mcp-test").toFile()
            .also { it.deleteOnExit() }

    companion object {
        private const val DATA_DIR = "tasks.dataDir"

        private const val TAG = "McpGraphTestCase"
    }
}
