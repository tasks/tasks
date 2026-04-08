package org.tasks.jobs

sealed class WorkResult {
    data object Success : WorkResult()
    data object Fail : WorkResult()
}
