package org.tasks.wear

interface WearRefresher {
    suspend fun refresh()
    suspend fun notifyTaskChanged(taskId: Long) {}
    suspend fun notifyTaskDeleted(taskId: Long) {}
}