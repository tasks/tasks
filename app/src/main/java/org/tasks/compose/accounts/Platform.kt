package org.tasks.compose.accounts

import android.content.Context
import org.tasks.R
import org.tasks.extensions.Context.openUri

val Platform.featureTitle: Int get() = when (this) {
    Platform.TASKS_ORG -> R.string.tasks_org_account
    Platform.DAVX5 -> R.string.davx5
    Platform.CALDAV -> R.string.caldav
    Platform.ETEBASE -> R.string.etesync
    Platform.DECSYNC_CC -> R.string.decsync
    else -> 0
}

fun Context.openUrl(platform: Platform) {
    val url = when (platform) {
        Platform.DAVX5 -> R.string.url_davx5
        Platform.DECSYNC_CC -> R.string.url_decsync
        else -> return
    }
    openUri(getString(url))
}
