package org.tasks.mcp

enum class McpToolGroup {
    Tasks,
    Lists,
    Tags,
    Places,
    Reminders,
    Accounts,
}

data class McpTool(
    val name: String,
    val group: McpToolGroup,
    val write: Boolean,
)

object McpToolCatalog {

    val tools: List<McpTool> = listOf(
        McpTool("list_tasks", McpToolGroup.Tasks, write = false),
        McpTool("create_tasks", McpToolGroup.Tasks, write = true),
        McpTool("update_tasks", McpToolGroup.Tasks, write = true),
        McpTool("complete_tasks", McpToolGroup.Tasks, write = true),
        McpTool("delete_task", McpToolGroup.Tasks, write = true),

        McpTool("list_task_lists", McpToolGroup.Lists, write = false),
        McpTool("create_task_list", McpToolGroup.Lists, write = true),
        McpTool("update_task_list", McpToolGroup.Lists, write = true),
        McpTool("delete_task_list", McpToolGroup.Lists, write = true),

        McpTool("list_tags", McpToolGroup.Tags, write = false),
        McpTool("create_tag", McpToolGroup.Tags, write = true),
        McpTool("update_tag", McpToolGroup.Tags, write = true),
        McpTool("delete_tag", McpToolGroup.Tags, write = true),
        McpTool("set_task_tags", McpToolGroup.Tags, write = true),

        McpTool("list_places", McpToolGroup.Places, write = false),
        McpTool("create_place", McpToolGroup.Places, write = true),
        McpTool("update_place", McpToolGroup.Places, write = true),
        McpTool("delete_place", McpToolGroup.Places, write = true),

        McpTool("list_reminders", McpToolGroup.Reminders, write = false),
        McpTool("set_task_reminders", McpToolGroup.Reminders, write = true),

        McpTool("list_accounts", McpToolGroup.Accounts, write = false),
    )

    val names: Set<String> = tools.map { it.name }.toSet()

    val readTools: Set<String> = tools.filterNot { it.write }.map { it.name }.toSet()

    val byGroup: List<Pair<McpToolGroup, List<McpTool>>> =
        McpToolGroup.entries.map { group -> group to tools.filter { it.group == group } }
}

class ToolPermissions private constructor(
    val mode: AccessMode,
    val enabled: Set<String>,
) {

    fun allows(tool: String): Boolean = tool in enabled

    val isWrite: Boolean = McpToolCatalog.tools.any { it.write && it.name in enabled }

    val isPartial: Boolean = enabled.size < McpToolCatalog.names.size

    companion object {
        fun of(mode: AccessMode, advanced: Set<String> = emptySet()): ToolPermissions =
            ToolPermissions(
                mode = mode,
                enabled = when (mode) {
                    AccessMode.ReadOnly -> McpToolCatalog.readTools
                    AccessMode.ReadWrite -> McpToolCatalog.names
                    AccessMode.Advanced -> advanced intersect McpToolCatalog.names
                },
            )
    }
}
