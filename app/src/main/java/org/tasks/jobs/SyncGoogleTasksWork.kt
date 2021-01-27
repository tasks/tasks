package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.data.GoogleTaskListDao
import org.tasks.gtasks.GoogleTaskSynchronizer
import org.tasks.preferences.Preferences

@HiltWorker
class SyncGoogleTasksWork @AssistedInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        localBroadcastManager: LocalBroadcastManager,
        private val preferences: Preferences,
        private val googleTaskListDao: GoogleTaskListDao,
        private val googleTaskSynchronizer: GoogleTaskSynchronizer
) : SyncWork(context, workerParams, firebase, localBroadcastManager, preferences) {

    override suspend fun enabled() = googleTaskListDao.getAccounts().isNotEmpty()

    override val syncStatus = R.string.p_sync_ongoing_google_tasks

    override suspend fun doSync() {
        if (preferences.isManualSort) {
            preferences.isPositionHackEnabled = true
        }
        googleTaskJobs().awaitAll()
    }

    private suspend fun googleTaskJobs(): List<Deferred<Unit>> = coroutineScope {
        googleTaskListDao
                .getAccounts()
                .mapIndexed { i, account ->
                    async(Dispatchers.IO) {
                        googleTaskSynchronizer.sync(account, i)
                    }
                }
    }
}