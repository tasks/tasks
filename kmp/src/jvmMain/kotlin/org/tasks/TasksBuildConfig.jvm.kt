package org.tasks

import org.tasks.kmp.JvmBuildConfig

actual object TasksBuildConfig {
    actual val DEBUG: Boolean = JvmBuildConfig.DEBUG
    actual val VERSION_NAME: String = JvmBuildConfig.VERSION_NAME
    actual val VERSION_CODE: Int = JvmBuildConfig.VERSION_CODE
}
