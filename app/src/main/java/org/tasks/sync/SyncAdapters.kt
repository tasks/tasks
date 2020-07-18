package org.tasks.sync

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskListDao
import org.tasks.jobs.WorkManager
import javax.inject.Inject

class SyncAdapters @Inject constructor(
        private val workManager: WorkManager,
        private val caldavDao: CaldavDao,
        private val googleTaskListDao: GoogleTaskListDao) {

    suspend fun sync() {
        sync(false)
    }

    suspend fun sync(immediate: Boolean): Boolean = withContext(NonCancellable) {
        if (isSyncEnabled()) {
            workManager.sync(immediate)
            true
        } else {
            false
        }
    }

    suspend fun isSyncEnabled(): Boolean = isGoogleTaskSyncEnabled() || isCaldavSyncEnabled()

    suspend fun isGoogleTaskSyncEnabled(): Boolean = googleTaskListDao.getAccounts().isNotEmpty()

    suspend fun isCaldavSyncEnabled(): Boolean = caldavDao.accountCount() > 0
}