package org.tasks.wear

import dagger.Lazy

/**
 * Bridge between the kmp WearSyncNotifier interface and the app-level WearRefresher.
 */
class WearSyncNotifierImpl(
    private val wearRefresher: Lazy<WearRefresher>,
) : WearSyncNotifier {
    override suspend fun notifyTaskChanged(taskId: Long) {
        wearRefresher.get().notifyTaskChanged(taskId)
    }

    override suspend fun notifyTaskDeleted(taskId: Long) {
        wearRefresher.get().notifyTaskDeleted(taskId)
    }
}
