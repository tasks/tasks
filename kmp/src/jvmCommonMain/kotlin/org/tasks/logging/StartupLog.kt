package org.tasks.logging

import co.touchlab.kermit.Logger
import org.tasks.TasksBuildConfig

private const val TAG = "Startup"

fun logStartup() = Logger.i(TAG) { startupMessage() }

internal fun startupMessage(): String {
    val buildType = if (TasksBuildConfig.DEBUG) "debug" else "release"
    val os = System.getProperty("os.name")
    val osVersion = System.getProperty("os.version")
    val arch = System.getProperty("os.arch")
    return "Tasks ${TasksBuildConfig.VERSION_NAME} " +
            "(build ${TasksBuildConfig.VERSION_CODE}, $buildType) " +
            "on $os $osVersion ($arch)"
}
