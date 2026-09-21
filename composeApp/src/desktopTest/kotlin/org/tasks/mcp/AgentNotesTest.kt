package org.tasks.mcp

import io.ktor.client.HttpClient
import io.ktor.http.HttpHeaders
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.api.AgentNotes
import java.net.ServerSocket
import java.time.ZoneId

class AgentNotesTest : McpGraphTestCase() {

    private var server: McpHttpServer? = null

    @After
    fun stopServer() {
        server?.takeIf { it.isRunning }?.stop()
        server = null
    }

    @Test
    fun readWriteInstructionsCarryEveryNoteWordForWord() = withClient(AccessMode.ReadWrite) { client ->
        val instructions = client.serverInstructions.orEmpty()

        AgentNotes.ALL.forEach {
            assertTrue("missing from the instructions: $it", instructions.contains(it))
        }
        assertTrue(instructions.contains(AgentNotes.timeZone(ZoneId.systemDefault().id)))
    }

    @Test
    fun readOnlyInstructionsDropTheNotesAboutWriting() = withClient(AccessMode.ReadOnly) { client ->
        val instructions = client.serverInstructions.orEmpty()

        AgentNotes.READS.forEach {
            assertTrue("missing from the instructions: $it", instructions.contains(it))
        }
        (AgentNotes.WRITES + AgentNotes.NEW_TASK_DEFAULTS + AgentNotes.DELETE_CASCADE).forEach {
            assertFalse("a read-only agent was told about writing: $it", instructions.contains(it))
        }
    }

    private fun withClient(mode: AccessMode, block: suspend (Client) -> Unit) = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val instance = McpHttpServer(
            port = port,
            token = { TOKEN },
            permissions = ToolPermissions.of(mode),
            api = api,
            log = ActivityLog(),
            onStateChange = { _, _ -> },
        )
        server = instance
        instance.start()

        val http = HttpClient()
        val client = Client(Implementation(name = "test-client", version = "1.0"))
        try {
            client.connect(
                StreamableHttpClientTransport(
                    client = http,
                    url = McpHttpServer.endpoint(port),
                ) { headers.append(HttpHeaders.Authorization, "Bearer $TOKEN") }
            )
            block(client)
        } finally {
            runCatching { client.close() }
            runCatching { http.close() }
            instance.stop()
            server = null
        }
    }

    private companion object {
        const val TOKEN = "test-token"
    }
}
