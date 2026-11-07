package org.tasks.billing

import org.tasks.PlatformConfiguration

fun restoreSources(configuration: PlatformConfiguration): List<UnlockSource> =
    UnlockSource.entries.filter {
        when (it) {
            UnlockSource.GOOGLE_PLAY -> configuration.supportsPlayLinkRestore
            UnlockSource.GITHUB -> true
            UnlockSource.STRIPE -> true
        }
    }
