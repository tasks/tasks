package org.tasks.compose.accounts

import android.content.Context
import org.tasks.R
import org.tasks.extensions.Context.openUri

fun Context.openUrl(platform: Platform) {
    val url = when (platform) {
        Platform.DAVX5 -> R.string.url_davx5
        else -> return
    }
    openUri(getString(url))
}
