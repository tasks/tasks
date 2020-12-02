package org.tasks.jobs

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import kotlinx.coroutines.*
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.data.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.CaldavDao
import org.tasks.etebase.EtebaseSynchronizer
import org.tasks.preferences.Preferences

class SyncEtebaseWork @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        localBroadcastManager: LocalBroadcastManager,
        preferences: Preferences,
        private val caldavDao: CaldavDao,
        private val synchronizer: EtebaseSynchronizer
) : SyncWork(context, workerParams, firebase, localBroadcastManager, preferences) {

    override suspend fun enabled() = caldavDao.getAccounts(TYPE_ETEBASE).isNotEmpty()

    override val syncStatus = R.string.p_sync_ongoing_etebase

    override suspend fun doSync() {
        jobs().awaitAll()
    }

    private suspend fun jobs(): List<Deferred<Unit>> = coroutineScope {
        caldavDao.getAccounts(TYPE_ETEBASE)
                .map {
                    async(Dispatchers.IO) {
                        synchronizer.sync(it)
                    }
                }
    }
}