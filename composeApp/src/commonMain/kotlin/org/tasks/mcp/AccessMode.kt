package org.tasks.mcp

enum class AccessMode {
    ReadOnly,
    ReadWrite,
    Advanced,
    ;

    companion object {
        fun of(stored: String?): AccessMode = entries.firstOrNull { it.name == stored } ?: ReadOnly
    }
}
