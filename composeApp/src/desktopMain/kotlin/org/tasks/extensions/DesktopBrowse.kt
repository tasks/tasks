package org.tasks.extensions

import co.touchlab.kermit.Logger
import java.awt.Desktop
import java.net.URI

private val logger = Logger.withTag("DesktopBrowse")

fun openInBrowser(url: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
            return
        }
    } catch (e: Exception) {
        logger.w(e) { "Desktop.browse() failed, falling back to command line" }
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
