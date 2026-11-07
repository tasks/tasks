package org.tasks.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.tasks.TasksBuildConfig
import org.tasks.preferences.TasksPreferences

val TasksPreferences.debugProFlow: Flow<Boolean>
    get() = if (TasksBuildConfig.DEBUG) {
        flow(TasksPreferences.debugPro, false)
    } else {
        flowOf(false)
    }
