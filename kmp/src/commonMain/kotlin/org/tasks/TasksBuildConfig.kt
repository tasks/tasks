package org.tasks

expect object TasksBuildConfig {
    val DEBUG: Boolean
    val VERSION_NAME: String
    val VERSION_CODE: Int
}
