package org.tasks.compose.accounts

import org.tasks.PlatformConfiguration

enum class Platform {
    TASKS_ORG,
    GOOGLE_TASKS,
    MICROSOFT,
    DAVX5,
    CALDAV,
    ETEBASE,
}

fun Platform.requiresPro(configuration: PlatformConfiguration): Boolean = when (this) {
    Platform.CALDAV,
    Platform.ETEBASE,
    Platform.DAVX5 -> true
    Platform.GOOGLE_TASKS,
    Platform.MICROSOFT -> configuration.thirdPartySyncRequiresPro
    Platform.TASKS_ORG -> false
}
