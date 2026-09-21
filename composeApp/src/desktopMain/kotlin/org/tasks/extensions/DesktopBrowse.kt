package org.tasks.extensions

import co.touchlab.kermit.Logger
import org.tasks.di.Platform
import org.tasks.di.platform
import java.awt.Desktop
import java.net.URI

private const val TAG = "DesktopBrowse"

private val logger = Logger.withTag(TAG)

fun openInBrowser(url: String): Boolean {
    try {
        val uri = URI(url)
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (uri.scheme == "mailto" && desktop.isSupported(Desktop.Action.MAIL)) {
                desktop.mail(uri)
                return true
            }
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri)
                return true
            }
        }
    } catch (e: Throwable) {
        logger.w(e) { "Desktop action failed, falling back to command line" }
    }
    val command = when (platform()) {
        Platform.LINUX -> arrayOf("xdg-open", url)
        Platform.MAC -> arrayOf("open", url)
        Platform.WINDOWS -> arrayOf("rundll32", "url.dll,FileProtocolHandler", url)
    }
    return try {
        ProcessBuilder(*command)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        true
    } catch (e: Throwable) {
        logger.w(e) { "Failed to open $url with ${command.first()}" }
        false
    }
}
