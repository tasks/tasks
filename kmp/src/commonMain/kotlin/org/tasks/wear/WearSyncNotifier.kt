package org.tasks.wear

/**
 * Interface for notifying the wear sync layer about task changes.
 * Implemented in the app module to bridge to the actual WearRefresher.
 */
interface WearSyncNotifier {
    suspend fun notifyTaskChanged(taskId: Long)
    suspend fun notifyTaskDeleted(taskId: Long)
}
