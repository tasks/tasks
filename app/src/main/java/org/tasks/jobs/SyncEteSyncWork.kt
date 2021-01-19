package org.tasks.jobs

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import kotlinx.coroutines.*
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.data.CaldavAccount.Companion.TYPE_ETESYNC
import org.tasks.data.CaldavDao
import org.tasks.etesync.EteSynchronizer
import org.tasks.preferences.Preferences

@Deprecated("use etebase")
class SyncEteSyncWork @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        localBroadcastManager: LocalBroadcastManager,
        preferences: Preferences,
        private val caldavDao: CaldavDao,
        private val eteSynchronizer: EteSynchronizer
) : SyncWork(context, workerParams, firebase, localBroadcastManager, preferences) {

    override suspend fun enabled() = caldavDao.getAccounts(TYPE_ETESYNC).isNotEmpty()

    override val syncStatus = R.string.p_sync_ongoing_etesync

    override suspend fun doSync() {
        firebase.logEvent(R.string.legacy_etesync)
        etesyncJobs().awaitAll()
    }

    private suspend fun etesyncJobs(): List<Deferred<Unit>> = coroutineScope {
        caldavDao.getAccounts(TYPE_ETESYNC)
                .map {
                    async(Dispatchers.IO) {
                        eteSynchronizer.sync(it)
                    }
                }
    }
}