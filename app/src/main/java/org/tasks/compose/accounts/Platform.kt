package org.tasks.compose.accounts

import org.tasks.R

enum class Platform {
    TASKS_ORG,
    GOOGLE_TASKS,
    MICROSOFT,
    DAVX5,
    CALDAV,
    ETEBASE,
    DECSYNC_CC,
    ;

    val featureTitle: Int get() = when (this) {
        TASKS_ORG -> R.string.tasks_org_account
        DAVX5 -> R.string.davx5
        CALDAV -> R.string.caldav
        ETEBASE -> R.string.etesync
        DECSYNC_CC -> R.string.decsync
        else -> 0
    }
}
