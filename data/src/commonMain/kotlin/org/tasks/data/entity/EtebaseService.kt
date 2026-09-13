package org.tasks.data.entity

/** Explicit service identity; unknown and legacy markers always mean EteSync. */
enum class EtebaseService(val serverType: Int, val defaultUrl: String) {
    ETESYNC(-1, "https://api.etebase.com/partner/tasksorg/"),
    SILENTSUITE(7, "https://server.silentsuite.io");

    fun effectiveUrl(url: String, showUrl: Boolean): String =
        if (showUrl) url.trim().ifEmpty { defaultUrl } else defaultUrl

    companion object {
        fun fromServerType(serverType: Int): EtebaseService =
            if (serverType == SILENTSUITE.serverType) SILENTSUITE else ETESYNC
    }
}
