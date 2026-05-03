package org.tasks.time

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

private val cached: Boolean by lazy {
    when {
        System.getProperty("os.name").lowercase().let { "mac" in it || "darwin" in it } ->
            macIs24Hour() ?: localeIs24Hour()
        else -> localeIs24Hour()
    }
}

@androidx.compose.runtime.Composable
actual fun is24HourFormat(): Boolean = cached

private fun macIs24Hour(): Boolean? = try {
    val process = ProcessBuilder("defaults", "read", "NSGlobalDomain", "AppleICUForce24HourTime")
        .redirectErrorStream(true)
        .start()
    val result = process.inputStream.bufferedReader().readText().trim()
    if (process.waitFor() == 0) result == "1" else null
} catch (_: Exception) {
    null
}

private fun localeIs24Hour(): Boolean {
    val pattern = (DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()) as? SimpleDateFormat)
        ?.toPattern() ?: return false
    return !pattern.contains("a")
}
