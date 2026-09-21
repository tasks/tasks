package org.tasks.mcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.tasks.api.ApiQueryEngine
import org.tasks.api.ApiWriter
import org.tasks.data.db.Database
import org.tasks.di.commonModule
import org.tasks.di.platformModule
import org.tasks.di.resetDirectories
import org.tasks.extensions.closeQuietly
import org.tasks.preferences.TasksPreferences
import java.net.ServerSocket

class McpServerWiringTest {
    private var koin: Koin? = null
    private var previousDataDir: String? = null

    @Before
    fun setUp() {
        previousDataDir = System.getProperty(DATA_DIR)
        System.setProperty(DATA_DIR, createTempDirectory().absolutePath)
        resetDirectories()
    }

    @After
    fun tearDown() {
        koin?.let { koin ->
            closeQuietly(TAG, "the MCP server") {
                runBlocking { koin.get<DesktopMcpServerController>().shutdown() }
            }
            closeQuietly(TAG, "the database") { koin.get<Database>().close() }
            closeQuietly(TAG, "the scope") { koin.get<CoroutineScope>().cancel() }
        }

        try {
            stopKoin()
        } finally {
            koin = null
            previousDataDir
                ?.let { System.setProperty(DATA_DIR, it) }
                ?: System.clearProperty(DATA_DIR)
            resetDirectories()
        }
    }

    @Test
    fun resolvesTheControllerBehindTheSettingsScreen() {
        val koin = start()

        val controller = koin.get<DesktopMcpServerController>()
        assertSame(controller, koin.get<McpServerController>())
        assertSame(controller, koin.get<DesktopMcpServerController>())
    }

    @Test
    fun resolvesTheApiChainBehindTheTools() {
        val koin = start()

        koin.get<ApiQueryEngine>()
        koin.get<ApiWriter>()
        assertSame(koin.get<DatabaseTasksApi>(), koin.get<DatabaseTasksApi>())
    }

    @Test
    fun aServerLeftSwitchedOnComesBackWhenTheAppRestarts() = runBlocking {
        val koin = start()
        val port = freePort()
        koin.get<TasksPreferences>().apply {
            set(TasksPreferences.mcpServerEnabled, true)
            set(TasksPreferences.mcpServerPort, port)
        }

        val controller = koin.get<DesktopMcpServerController>()
        controller.initialize()

        assertTrue(controller.state.value.isRunning)
        assertEquals(port, controller.state.value.port)
    }

    @Test
    fun aServerLeftSwitchedOffStaysOff() = runBlocking {
        val koin = start()
        koin.get<TasksPreferences>().set(TasksPreferences.mcpServerEnabled, false)

        val controller = koin.get<DesktopMcpServerController>()
        controller.initialize()

        assertFalse(controller.state.value.isRunning)
    }

    @Test
    fun startupFillsInTheEndpointAndTheToken() = runBlocking {
        val koin = start()
        val port = freePort()
        koin.get<TasksPreferences>().apply {
            set(TasksPreferences.mcpServerEnabled, true)
            set(TasksPreferences.mcpServerPort, port)
        }

        val controller = koin.get<DesktopMcpServerController>()
        controller.initialize()

        val state = controller.state.value
        assertEquals("http://127.0.0.1:$port/mcp", state.endpoint)
        assertTrue(state.authorizationHeader, state.authorizationHeader.startsWith("Bearer "))
        assertEquals(3, state.clients.size)
        state.clients.forEach {
            assertTrue("${it.id} is missing the port", it.snippet.contains("$port"))
        }
    }

    @Test
    fun theShownSnippetsAreMaskedAndTheCopiedOnesAreNot() = runBlocking {
        val koin = start()
        koin.get<TasksPreferences>().set(TasksPreferences.mcpServerPort, freePort())

        val controller = koin.get<DesktopMcpServerController>()
        controller.initialize()

        val state = controller.state.value
        val token = state.authorizationHeader.removePrefix("Bearer ")
        assertTrue(token.length > 16)

        state.clients.forEach {
            assertTrue("${it.id} does not carry the token", it.snippet.contains(token))
            assertFalse("${it.id} leaks the token on screen", it.displaySnippet.contains(token))
            assertTrue("${it.id} lost its endpoint", it.displaySnippet.contains(state.endpoint))
        }
        assertFalse(state.maskedAuthorizationHeader.contains(token))
        assertTrue(state.maskedAuthorizationHeader.startsWith("Bearer "))
        val shown = state.maskedAuthorizationHeader.removePrefix("Bearer ")
        assertTrue(shown, shown.endsWith(token.takeLast(4)))
        assertTrue(shown, shown.dropLast(4).all { it == '\u2022' })
        assertFalse("only the tail may show", shown.contains(token.dropLast(4)))
        assertNotEquals(token.length, shown.length)
    }

    @Test
    fun anExistingReadWriteChoiceSurvivesTheNewSetting() = runBlocking {
        val koin = start()
        koin.get<TasksPreferences>().set(TasksPreferences.mcpServerReadWrite, true)

        val controller = koin.get<DesktopMcpServerController>()
        controller.initialize()

        assertEquals(AccessMode.ReadWrite, controller.state.value.mode)
        assertEquals(McpToolCatalog.names, controller.state.value.exposedTools)
    }

    @Test
    fun anAbsentToolListFallsBackToTheModesOwnTools() = runBlocking {
        val koin = start()
        koin.get<TasksPreferences>()
            .set(TasksPreferences.mcpServerAccessMode, AccessMode.ReadWrite.name)

        val controller = koin.get<DesktopMcpServerController>()
        controller.initialize()

        assertEquals(McpToolCatalog.names, controller.state.value.enabledTools)
    }

    @Test
    fun anEmptiedToolListStaysEmpty() = runBlocking {
        val koin = start()
        koin.get<TasksPreferences>().apply {
            set(TasksPreferences.mcpServerAccessMode, AccessMode.Advanced.name)
            set(TasksPreferences.mcpServerEnabledTools, emptySet())
        }

        val controller = koin.get<DesktopMcpServerController>()
        controller.initialize()

        assertEquals(emptySet<String>(), controller.state.value.enabledTools)
        assertEquals(emptySet<String>(), controller.state.value.exposedTools)
    }

    @Test
    fun advancedOpensOnWhatThePresetBeforeItExposed() = runBlocking {
        val koin = start()
        val controller = koin.get<DesktopMcpServerController>()
        controller.initialize()
        controller.setAccessMode(AccessMode.ReadOnly)
        controller.await { it.enabledTools == McpToolCatalog.readTools }

        controller.setAccessMode(AccessMode.Advanced)

        val state = controller.await { it.mode == AccessMode.Advanced }
        assertEquals(McpToolCatalog.readTools, state.enabledTools)
        assertFalse(state.exposedTools.contains("delete_task"))
    }

    @Test
    fun changingTheToolListRestartsTheServerRatherThanStoppingIt() = runBlocking {
        val koin = start()
        koin.get<TasksPreferences>().apply {
            set(TasksPreferences.mcpServerEnabled, true)
            set(TasksPreferences.mcpServerPort, freePort())
            set(TasksPreferences.mcpServerAccessMode, AccessMode.Advanced.name)
        }
        val controller = koin.get<DesktopMcpServerController>()
        controller.initialize()
        assertTrue(controller.state.value.isRunning)

        controller.setToolsEnabled(setOf("list_tasks", "list_tags"), enabled = false)

        val state = controller.await { !it.enabledTools.contains("list_tasks") }
        assertTrue("the server must come back up", controller.await { it.isRunning }.isRunning)
        assertFalse(state.exposedTools.contains("list_tasks"))
    }

    private suspend fun McpServerController.await(
        predicate: (McpServerState) -> Boolean,
    ): McpServerState = withTimeout(AWAIT_TIMEOUT_MS) { state.first(predicate) }

    private fun start(): Koin =
        startKoin { modules(commonModule, platformModule()) }.koin.also { koin = it }

    private fun createTempDirectory() =
        java.nio.file.Files.createTempDirectory("tasks-mcp-di-test").toFile()
            .also { it.deleteOnExit() }

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    companion object {
        private const val DATA_DIR = "tasks.dataDir"

        private const val TAG = "McpServerWiringTest"

        private const val AWAIT_TIMEOUT_MS = 5_000L
    }
}
