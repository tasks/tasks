package org.tasks.mcp

import kotlinx.coroutines.flow.StateFlow

interface McpServerController {

    val state: StateFlow<McpServerState>

    fun setEnabled(enabled: Boolean)

    fun setAccessMode(mode: AccessMode)

    fun setToolsEnabled(names: Set<String>, enabled: Boolean)

    fun regenerateToken()
}

data class McpServerState(
    val enabled: Boolean = false,
    val status: Status = Status.Stopped,
    val port: Int = 0,
    val mode: AccessMode = AccessMode.ReadOnly,
    val enabledTools: Set<String> = emptySet(),
    val clients: List<McpClientSnippet> = emptyList(),
    val endpoint: String = "",
    val authorizationHeader: String = "",
    val maskedAuthorizationHeader: String = "",
    val error: String? = null,
) {
    enum class Status { Stopped, Starting, Running, Error }

    val isRunning: Boolean get() = status == Status.Running

    val exposedTools: Set<String> get() = ToolPermissions.of(mode, enabledTools).enabled
}

data class McpClientSnippet(
    val id: String,
    val name: String,
    val target: String,
    val snippet: String,
    val displaySnippet: String,
)
