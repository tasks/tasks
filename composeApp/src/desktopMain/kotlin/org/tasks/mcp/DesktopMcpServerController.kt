package org.tasks.mcp

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tasks.preferences.TasksPreferences
import org.tasks.security.KeyStoreEncryption
import java.security.SecureRandom

class DesktopMcpServerController(
    private val preferences: TasksPreferences,
    private val encryption: KeyStoreEncryption,
    private val api: DatabaseTasksApi,
    private val analytics: org.tasks.analytics.Analytics,
    private val scope: CoroutineScope,
) : McpServerController {

    private val logger = Logger.withTag(TAG)
    private val lock = Mutex()

    private val _state = MutableStateFlow(McpServerState())
    override val state: StateFlow<McpServerState> = _state.asStateFlow()

    val log = ActivityLog()

    private var server: McpHttpServer? = null
    private var token: String = ""

    suspend fun initialize() = lock.withLock {
        token = loadOrCreateToken()
        val enabled = preferences.get(TasksPreferences.mcpServerEnabled, false)
        val port = preferences.get(TasksPreferences.mcpServerPort, McpHttpServer.DEFAULT_PORT)
        val mode = loadMode()
        _state.update {
            it.copy(
                enabled = enabled,
                port = port,
                mode = mode,
                enabledTools = preferences.get(
                    TasksPreferences.mcpServerEnabledTools,
                    ToolPermissions.of(mode).enabled,
                ),
            ).withConnection(port)
        }
        if (enabled) startLocked()
    }

    private suspend fun loadMode(): AccessMode {
        val stored = preferences.get(TasksPreferences.mcpServerAccessMode, "")
        if (stored.isNotBlank()) return AccessMode.of(stored)
        val readWrite = preferences.get(TasksPreferences.mcpServerReadWrite, false)
        return if (readWrite) AccessMode.ReadWrite else AccessMode.ReadOnly
    }

    override fun setEnabled(enabled: Boolean) {
        scope.launch {
            lock.withLock {
                preferences.set(TasksPreferences.mcpServerEnabled, enabled)
                _state.update { it.copy(enabled = enabled, error = null) }
                if (enabled) startLocked() else stopLocked()
            }
        }
    }

    override fun setAccessMode(mode: AccessMode) {
        scope.launch {
            lock.withLock {
                preferences.set(TasksPreferences.mcpServerAccessMode, mode.name)
                if (mode != AccessMode.Advanced) {
                    storeToolsLocked(ToolPermissions.of(mode).enabled)
                }
                _state.update { it.copy(mode = mode) }
                restartLocked()
            }
        }
    }

    override fun setToolsEnabled(names: Set<String>, enabled: Boolean) {
        scope.launch {
            lock.withLock {
                val current = _state.value.enabledTools
                storeToolsLocked(if (enabled) current + names else current - names)
                restartLocked()
            }
        }
    }

    private suspend fun storeToolsLocked(tools: Set<String>) {
        val stored = tools intersect McpToolCatalog.names
        preferences.set(TasksPreferences.mcpServerEnabledTools, stored)
        _state.update { it.copy(enabledTools = stored) }
    }

    private fun restartLocked() {
        if (server == null) return
        stopLocked()
        startLocked()
    }

    override fun regenerateToken() {
        scope.launch {
            lock.withLock {
                token = generateToken()
                storeToken(token)
                _state.update { it.withConnection(it.port) }
            }
        }
    }

    suspend fun shutdown() = lock.withLock { stopLocked() }

    private fun startLocked() {
        if (server != null) return
        val current = _state.value
        val permissions = ToolPermissions.of(current.mode, current.enabledTools)
        _state.update { it.copy(status = McpServerState.Status.Starting, error = null) }
        val instance = McpHttpServer(
            port = current.port,
            token = { token },
            permissions = permissions,
            api = api,
            log = log,
            analytics = analytics,
            onStateChange = { running, error ->
                _state.update {
                    it.copy(
                        status = when {
                            error != null -> McpServerState.Status.Error
                            running -> McpServerState.Status.Running
                            else -> McpServerState.Status.Stopped
                        },
                        error = error,
                    )
                }
            },
        )
        server = instance
        try {
            instance.start()
            logger.i { "MCP server listening on ${McpHttpServer.endpoint(current.port)}" }
        } catch (e: Throwable) {
            server = null
            logger.w(e) { "Failed to start the MCP server" }
        }
    }

    private fun stopLocked() {
        server?.let {
            runCatching { it.stop() }.onFailure { e -> logger.w(e) { "Failed to stop cleanly" } }
        }
        server = null
        _state.update { it.copy(status = McpServerState.Status.Stopped) }
    }

    private fun McpServerState.withConnection(port: Int): McpServerState {
        val endpoint = McpHttpServer.endpoint(port)
        val masked = maskToken(token)
        return copy(
            endpoint = endpoint,
            authorizationHeader = "Bearer $token",
            maskedAuthorizationHeader = "Bearer $masked",
            clients = mcpClientSetups(endpoint, token).map {
                McpClientSnippet(
                    id = it.id,
                    name = it.name,
                    target = it.target,
                    snippet = it.snippet,
                    displaySnippet = it.snippet.replace(token, masked),
                )
            },
        )
    }

    private suspend fun loadOrCreateToken(): String {
        val stored = preferences.get(TasksPreferences.mcpServerToken, "")
        if (stored.isNotBlank()) {
            encryption.decrypt(stored)?.takeIf { it.isNotBlank() }?.let { return it }
            logger.w { "Could not read the stored MCP token, generating a new one" }
        }
        return generateToken().also { storeToken(it) }
    }

    private suspend fun storeToken(value: String) {
        val encrypted = encryption.encrypt(value)
        if (encrypted == null) {
            logger.w { "Could not encrypt the MCP token, it will not survive a restart" }
            preferences.set(TasksPreferences.mcpServerToken, "")
        } else {
            preferences.set(TasksPreferences.mcpServerToken, encrypted)
        }
    }

    private companion object {
        const val TAG = "McpServer"

        const val VISIBLE_TOKEN_CHARS = 4

        fun maskToken(token: String): String {
            val tail = token.takeIf { it.length > VISIBLE_TOKEN_CHARS * 3 }
                ?.takeLast(VISIBLE_TOKEN_CHARS)
                .orEmpty()
            return "\u2022".repeat(16) + tail
        }

        fun generateToken(): String {
            val bytes = ByteArray(20).also { SecureRandom().nextBytes(it) }
            val alphabet = "abcdefghijklmnopqrstuvwxyz234567"
            return buildString {
                bytes.forEachIndexed { i, b ->
                    if (i > 0 && i % 4 == 0) append('-')
                    append(alphabet[(b.toInt() and 0xFF) % alphabet.length])
                    append(alphabet[(b.toInt() shr 3 and 0x1F)])
                }
            }
        }
    }
}
