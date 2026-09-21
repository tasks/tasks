package org.tasks.notifications

import org.tasks.di.Platform
import org.tasks.di.platform
import java.io.File

internal const val NO_SERVER_ASSIGNED_IDS = "n/a"

internal const val BOOT_ID_PATH = "/proc/sys/kernel/random/boot_id"

internal fun notificationSessionToken(
    platform: Platform = platform(),
    bootId: File = File(BOOT_ID_PATH),
): String? = when (platform) {
    Platform.LINUX -> readBootId(bootId)
    Platform.WINDOWS, Platform.MAC -> NO_SERVER_ASSIGNED_IDS
}

private fun readBootId(source: File): String? = runCatching {
    source.takeIf { it.canRead() }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
}.getOrNull()
