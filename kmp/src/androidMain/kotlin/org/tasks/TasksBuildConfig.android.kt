package org.tasks

import org.tasks.kmp.BuildConfig

actual object TasksBuildConfig {
    actual val DEBUG: Boolean = BuildConfig.DEBUG
    actual val VERSION_NAME: String = BuildConfig.VERSION_NAME
    actual val VERSION_CODE: Int = BuildConfig.VERSION_CODE
}
