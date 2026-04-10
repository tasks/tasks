package org.tasks

import org.tasks.kmp.JvmBuildConfig

actual object TasksBuildConfig {
    actual val DEBUG: Boolean = JvmBuildConfig.DEBUG
}
