package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import org.tasks.PlatformConfiguration
import org.tasks.TasksBuildConfig

open class MainSettingsViewModel(
    private val platformConfiguration: PlatformConfiguration,
) : ViewModel() {
    open val supportsWidgets: Boolean
        get() = platformConfiguration.supportsWidgets

    val isDebug: Boolean
        get() = TasksBuildConfig.DEBUG
}
