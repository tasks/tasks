package org.tasks.mcp

data class McpClientSetup(
    val id: String,
    val name: String,
    val target: String,
    val tagline: String,
    val configPath: String?,
    val snippet: String,
    val verify: String,
    val notes: List<String> = emptyList(),
)

const val MCP_SERVER_KEY = "tasks"

fun mcpClientSetups(url: String, token: String): List<McpClientSetup> = listOf(
    McpClientSetup(
        id = "claude-code",
        name = "Claude Code",
        target = "Run in a terminal",
        tagline = "claude mcp add",
        configPath = null,
        snippet = "claude mcp add --transport http $MCP_SERVER_KEY \\\n" +
                "  $url \\\n" +
                "  --header \"Authorization: Bearer $token\"",
        verify = "claude mcp list",
        notes = listOf(
            "Add --scope user to make the server available in every project rather than " +
                    "just the current directory.",
        ),
    ),

    McpClientSetup(
        id = "codex",
        name = "OpenAI Codex",
        target = "~/.codex/config.toml",
        tagline = "~/.codex/config.toml",
        configPath = "~/.codex/config.toml",
        snippet = buildString {
            appendLine("[mcp_servers.$MCP_SERVER_KEY]")
            appendLine("url = \"$url\"")
            appendLine("http_headers = { Authorization = \"Bearer $token\" }")
        }.trim(),
        verify = "codex mcp list",
        notes = listOf(
            "Codex reads its MCP servers at startup, so restart it after editing the config.",
            "Codex rejects a bare bearer_token key on HTTP transports - the token has to go " +
                    "through http_headers, as above.",
            "To keep the token out of the config file, use " +
                    "bearer_token_env_var = \"TASKS_MCP_TOKEN\" instead and export it.",
        ),
    ),

    McpClientSetup(
        id = "gemini-cli",
        name = "Gemini CLI",
        target = "~/.gemini/settings.json",
        tagline = "~/.gemini/settings.json",
        configPath = "~/.gemini/settings.json",
        snippet = buildString {
            appendLine("{")
            appendLine("  \"mcpServers\": {")
            appendLine("    \"$MCP_SERVER_KEY\": {")
            appendLine("      \"httpUrl\": \"$url\",")
            appendLine("      \"headers\": {")
            appendLine("        \"Authorization\": \"Bearer $token\"")
            appendLine("      }")
            appendLine("    }")
            appendLine("  }")
            append("}")
        },
        verify = "/mcp",
        notes = listOf(
            "Use httpUrl, not url. The url key selects the deprecated HTTP+SSE transport, " +
                    "which this server does not serve.",
            "If you already have an mcpServers block, merge the \"$MCP_SERVER_KEY\" entry " +
                    "into it rather than adding a second block.",
        ),
    ),
)

fun mcpHealthProbe(baseUrl: String): String = "curl -s $baseUrl/health"

fun mcpToolsListProbe(url: String, token: String): String = buildString {
    appendLine("curl -sN $url \\")
    appendLine("  -H 'Authorization: Bearer $token' \\")
    appendLine("  -H 'Content-Type: application/json' \\")
    appendLine("  -H 'Accept: application/json, text/event-stream' \\")
    appendLine("  -d '{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}'")
}.trim()
