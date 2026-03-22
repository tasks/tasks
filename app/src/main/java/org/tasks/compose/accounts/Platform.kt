package org.tasks.compose.accounts

import org.tasks.R

val Platform.featureTitle: Int get() = when (this) {
    Platform.TASKS_ORG -> R.string.tasks_org_account
    Platform.DAVX5 -> R.string.davx5
    Platform.CALDAV -> R.string.caldav
    Platform.ETEBASE -> R.string.etesync
    Platform.DECSYNC_CC -> R.string.decsync
    else -> 0
}
