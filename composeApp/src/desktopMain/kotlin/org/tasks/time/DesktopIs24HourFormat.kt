package org.tasks.time

import org.tasks.di.Platform
import org.tasks.di.platform
import org.tasks.process.runToCompletion
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "Is24HourFormat"

private const val TIMEOUT_SECONDS = 1L

private val cached: Boolean by lazy {
    when (platform()) {
        Platform.MAC -> macIs24Hour() ?: localeIs24Hour()
        Platform.WINDOWS, Platform.LINUX -> localeIs24Hour()
    }
}

@androidx.compose.runtime.Composable
actual fun is24HourFormat(): Boolean = cached

private fun macIs24Hour(): Boolean? =
    ProcessBuilder("defaults", "read", "NSGlobalDomain", "AppleICUForce24HourTime")
        .redirectErrorStream(true)
        .runToCompletion(TIMEOUT_SECONDS, TAG, "24-hour format")
        ?.takeIf { it.exitCode == 0 }
        ?.let { it.output.trim() == "1" }

private fun localeIs24Hour(): Boolean {
    val pattern = (DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()) as? SimpleDateFormat)
        ?.toPattern() ?: return false
    return !pattern.contains("a")
}
