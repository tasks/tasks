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

internal fun supportsSystemNotificationSettings(): Boolean =
    systemNotificationSettingsCommand() != null

private fun systemNotificationSettingsCommand(): List<String>? = when (platform()) {
    Platform.MAC ->
        listOf(
            "open",
            "x-apple.systempreferences:$macNotificationsPane?id=${TasksBuildConfig.APPLICATION_ID}",
        )
    Platform.WINDOWS ->
        listOf("cmd", "/c", "start", "", "ms-settings:notifications")
    Platform.LINUX -> linuxCandidates().firstOrNull { onPath(it.first()) }
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

private class DesktopSettings(val desktops: Set<String>, val commands: List<List<String>>)

private val LINUX_SETTINGS = listOf(
    DesktopSettings(
        setOf("kde", "plasma"),
        listOf(
            listOf("systemsettings", "kcm_notifications"),
            listOf("systemsettings5", "kcm_notifications"),
            listOf("kcmshell6", "kcm_notifications"),
            listOf("kcmshell5", "kcm_notifications"),
        ),
    ),
    DesktopSettings(
        setOf("gnome", "unity"),
        listOf(listOf("gnome-control-center", "notifications")),
    ),
    DesktopSettings(
        setOf("xfce"),
        listOf(listOf("xfce4-notifyd-config")),
    ),
    DesktopSettings(
        setOf("cinnamon"),
        listOf(listOf("cinnamon-settings", "notifications")),
    ),
    DesktopSettings(
        setOf("mate"),
        listOf(listOf("mate-notification-properties")),
    ),
    DesktopSettings(
        setOf("lxqt"),
        listOf(listOf("lxqt-config-notificationd")),
    ),
)

private fun linuxCandidates(): List<List<String>> {
    val desktop = (System.getenv("XDG_CURRENT_DESKTOP") ?: System.getenv("DESKTOP_SESSION"))
        .orEmpty()
        .lowercase()
    val (matching, rest) = LINUX_SETTINGS.partition { settings ->
        settings.desktops.any { it in desktop }
    }
    return (matching + rest).flatMap { it.commands }
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
        val process = try {
            ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (e: Exception) {
            logger.w(e) { "Failed to start $command" }
            return@launch
        }
        process.onExit().thenAccept { finished ->
            val exitCode = finished.exitValue()
            if (exitCode != 0) {
                logger.w { "$command exited with $exitCode" }
            }
        }
    }
}
