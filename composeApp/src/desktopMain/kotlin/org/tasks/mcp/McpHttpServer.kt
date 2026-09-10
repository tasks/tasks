package org.tasks.mcp

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import java.security.MessageDigest
import org.tasks.api.AgentNotes
import java.time.ZoneId

class McpHttpServer(
    private val port: Int,
    private val token: () -> String,
    private val permissions: ToolPermissions,
    private val api: DatabaseTasksApi,
    private val log: ActivityLog,
    private val onStateChange: (Boolean, String?) -> Unit,
    private val analytics: org.tasks.analytics.Analytics? = null,
) {
    private var engine: EmbeddedServer<*, *>? = null

    val isRunning: Boolean get() = engine != null

    fun start() {
        check(engine == null) { "Server already running" }

        val server = embeddedServer(CIO, port = port, host = LOOPBACK) {
            installAuth()

            routing {
                get(HEALTH_PATH) {
                    call.respondText(
                        """{"status":"ok","name":"$SERVER_NAME","version":"$SERVER_VERSION"}""",
                        ContentType.Application.Json,
                    )
                }
            }

            mcpStreamableHttp(
                path = MCP_PATH,
                enableDnsRebindingProtection = true,
            ) {
                buildMcpServer()
            }
        }

        engine = server
        try {
            server.start(wait = false)
            onStateChange(true, null)
        } catch (e: Throwable) {
            engine = null
            onStateChange(false, e.message ?: e::class.simpleName ?: "Failed to start")
            throw e
        }
    }

    fun stop() {
        engine?.stop(gracePeriodMillis = 300, timeoutMillis = 1_500)
        engine = null
        onStateChange(false, null)
    }

    private fun io.ktor.server.application.Application.installAuth() {
        intercept(ApplicationCallPipeline.Plugins) {
            if (call.request.path() == HEALTH_PATH) return@intercept

            val expected = token()
            val presented = call.request.header(HttpHeaders.Authorization)
                ?.removePrefix("Bearer ")
                ?.trim()

            if (presented == null || !constantTimeEquals(presented, expected)) {
                call.response.header(
                    HttpHeaders.WWWAuthenticate,
                    """Bearer realm="Tasks.org", error="invalid_token"""",
                )
                log.record(
                    tool = "auth",
                    summary = if (presented == null) {
                        "Rejected a request with no bearer token"
                    } else {
                        "Rejected a request with an invalid bearer token"
                    },
                    outcome = ActivityLog.Outcome.Denied,
                    durationMs = 0,
                )
                call.respondText(
                    """{"error":"unauthorized","detail":"Set Authorization: Bearer <token> using the command shown in Tasks.org settings."}""",
                    ContentType.Application.Json,
                    HttpStatusCode.Unauthorized,
                )
                finish()
            }
        }
    }

    private fun buildMcpServer(): Server {
        val server = Server(
            serverInfo = Implementation(
                name = SERVER_NAME,
                version = SERVER_VERSION,
                title = "Tasks.org",
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = false),
                ),
            ),
            instructions = instructions(),
        )
        server.registerTasksTools(api, permissions, log, analytics)
        server.onConnect {
            log.record(
                tool = "connect",
                summary = "MCP client connected (${permissions.mode.name})",
                outcome = ActivityLog.Outcome.Ok,
                durationMs = 0,
            )
        }
        return server
    }

    private fun instructions(): String = buildString {
        appendLine("Read and manage the user's tasks in Tasks.org on this computer.")
        appendLine()
        appendLine("Access level: ${if (permissions.isWrite) "read and write" else "READ ONLY"}.")
        if (!permissions.isWrite) {
            appendLine(
                "The user has not granted write access, so no tool here can change " +
                    "anything. Do not tell the user you have created or changed a task.",
            )
        }
        if (permissions.isPartial) {
            appendLine(
                "The user has also switched individual tools off. What is not in tools/list " +
                    "is not available to you: say so plainly and stop, rather than working " +
                    "around it, inferring it from another tool, or asking them to turn it " +
                    "back on.",
            )
        }
        appendLine()
        appendLine("Things about this data model that are easy to get wrong:")
        appendLine()
        AgentNotes.READS.forEach { appendLine("- $it") }
        appendLine("- ${AgentNotes.timeZone(ZoneId.systemDefault().id)}")
        appendLine("- Every read tool takes `fields`, naming the columns to return. Use it: a")
        appendLine("  hundred tasks cost a fraction as much when you ask only for the two")
        appendLine("  fields you are going to read, and skipping a name field skips the query")
        appendLine("  behind it. `id` always comes back, and a field you name always comes")
        appendLine("  back - empty ones as null or false rather than missing.")
        appendLine("- A page can also be cut short to fit the response size limit, so a short")
        appendLine("  page is not the end. Trust `has_more` and continue from `next_offset`.")
        if (permissions.isWrite) {
            AgentNotes.WRITES.forEach { appendLine("- $it") }
        }
        if (permissions.allows("create_tasks")) {
            appendLine("- ${AgentNotes.NEW_TASK_DEFAULTS}")
        }
        if (permissions.allows("delete_task")) {
            appendLine("- ${AgentNotes.DELETE_CASCADE}")
        }
    }

    companion object {
        const val SERVER_NAME = "tasks-org"
        const val SERVER_VERSION = "0.1.0"
        const val MCP_PATH = "/mcp"
        const val HEALTH_PATH = "/health"
        const val LOOPBACK = "127.0.0.1"
        const val DEFAULT_PORT = 8808

        fun baseUrl(port: Int) = "http://$LOOPBACK:$port"

        fun endpoint(port: Int) = "${baseUrl(port)}$MCP_PATH"

        private fun constantTimeEquals(a: String, b: String): Boolean {
            val da = MessageDigest.getInstance("SHA-256").digest(a.toByteArray())
            val db = MessageDigest.getInstance("SHA-256").digest(b.toByteArray())
            return MessageDigest.isEqual(da, db)
        }
    }
}
