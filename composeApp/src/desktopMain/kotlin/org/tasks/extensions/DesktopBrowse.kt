package org.tasks.extensions

import co.touchlab.kermit.Logger
import java.awt.Desktop
import java.net.URI

private val logger = Logger.withTag("DesktopBrowse")

fun openInBrowser(url: String) {
    val uri = URI(url)
    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (uri.scheme == "mailto" && desktop.isSupported(Desktop.Action.MAIL)) {
                desktop.mail(uri)
                return
            }
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri)
                return
            }
        }
    } catch (e: Exception) {
        logger.w(e) { "Desktop action failed, falling back to command line" }
    }
    val os = System.getProperty("os.name").lowercase()
    val command = when {
        "linux" in os -> arrayOf("xdg-open", url)
        "mac" in os || "darwin" in os -> arrayOf("open", url)
        "win" in os -> arrayOf("cmd", "/c", "start", url)
        else -> throw UnsupportedOperationException("Cannot open browser on $os")
    }
    Runtime.getRuntime().exec(command)
}
