package org.tasks

import org.tasks.kmp.IosBuildConfig

actual object TasksBuildConfig {
    actual val DEBUG: Boolean = IosBuildConfig.DEBUG
    actual val VERSION_NAME: String = IosBuildConfig.VERSION_NAME
    actual val VERSION_CODE: Int = IosBuildConfig.VERSION_CODE
    actual val APPLICATION_ID: String = IosBuildConfig.APPLICATION_ID
}
