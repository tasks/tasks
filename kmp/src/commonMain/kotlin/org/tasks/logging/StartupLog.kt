package org.tasks.logging

import co.touchlab.kermit.Logger
import org.tasks.TasksBuildConfig
import org.tasks.kmp.osDescription

private const val TAG = "Startup"

fun logStartup() = Logger.i(TAG) { startupMessage() }

internal fun startupMessage(): String {
    val buildType = if (TasksBuildConfig.DEBUG) "debug" else "release"
    return "Tasks ${TasksBuildConfig.VERSION_NAME} " +
            "(build ${TasksBuildConfig.VERSION_CODE}, $buildType) " +
            "on ${osDescription()}"
}
