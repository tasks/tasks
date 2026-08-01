package org.tasks.extensions

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.tasks.TasksBuildConfig
import org.tasks.di.Platform
import org.tasks.di.platform
import java.io.File

private val logger = Logger.withTag("SystemNotificationSettings")

internal fun supportsSystemNotificationSettings(): Boolean = when (platform()) {
    Platform.MAC, Platform.WINDOWS -> true
    Platform.LINUX -> linuxCandidates() != null
}

private fun systemNotificationSettingsCommand(): List<String>? = when (platform()) {
    Platform.MAC ->
        listOf(
            "open",
            "x-apple.systempreferences:$macNotificationsPane?id=${TasksBuildConfig.APPLICATION_ID}",
        )
    Platform.WINDOWS ->
        listOf("cmd", "/c", "start", "", "ms-settings:notifications")
    Platform.LINUX -> linuxCandidates()?.firstOrNull { onPath(it.first()) }
}

private val macNotificationsPane: String
    get() {
        val major = System.getProperty("os.version")
            ?.substringBefore('.')
            ?.toIntOrNull()
            ?: 0
        return if (major >= 13) {
            "com.apple.Notifications-Settings.extension"
        } else {
            "com.apple.preference.notifications"
        }
    }

private val GNOME_COMMANDS = listOf(
    listOf("gnome-control-center", "notifications"),
)

private val KDE_COMMANDS = listOf(
    listOf("systemsettings", "kcm_notifications"),
    listOf("systemsettings5", "kcm_notifications"),
    listOf("kcmshell6", "kcm_notifications"),
    listOf("kcmshell5", "kcm_notifications"),
)

private fun linuxCandidates(): List<List<String>>? {
    val desktop = (System.getenv("XDG_CURRENT_DESKTOP") ?: System.getenv("DESKTOP_SESSION"))
        .orEmpty()
        .lowercase()
    return when {
        "kde" in desktop || "plasma" in desktop -> KDE_COMMANDS
        "gnome" in desktop || "unity" in desktop -> GNOME_COMMANDS
        else -> null
    }
}

private fun onPath(binary: String): Boolean =
    System.getenv("PATH")
        ?.split(File.pathSeparator)
        ?.any { File(it, binary).canExecute() }
        ?: false

actual fun openSystemNotificationSettings() {
    KoinPlatform.getKoin().get<CoroutineScope>().launch(Dispatchers.IO) {
        val command = systemNotificationSettingsCommand()
        if (command == null) {
            logger.w { "No system notification settings command available" }
            return@launch
        }
        try {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            process.inputStream.bufferedReader().use { reader ->
                reader.lineSequence().forEach { logger.d { "${command.first()}: $it" } }
            }
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                logger.w { "$command exited with $exitCode" }
            }
        } catch (e: Exception) {
            logger.w(e) { "Failed to open notification settings: $command" }
        }
    }
}
