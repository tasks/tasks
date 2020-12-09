package org.tasks.jobs

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import kotlinx.coroutines.*
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.caldav.CaldavSynchronizer
import org.tasks.data.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.CaldavDao
import org.tasks.preferences.Preferences

class SyncCaldavWork @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        localBroadcastManager: LocalBroadcastManager,
        preferences: Preferences,
        private val caldavDao: CaldavDao,
        private val caldavSynchronizer: CaldavSynchronizer,
        private val inventory: Inventory
) : SyncWork(context, workerParams, firebase, localBroadcastManager, preferences) {

    override suspend fun enabled() = getAccounts().isNotEmpty()

    override val syncStatus = R.string.p_sync_ongoing_caldav

    override suspend fun doSync() {
        caldavJobs().awaitAll()
        inventory.updateTasksSubscription()
    }

    private suspend fun caldavJobs(): List<Deferred<Unit>> = coroutineScope {
        getAccounts().map {
            async(Dispatchers.IO) {
                caldavSynchronizer.sync(it)
            }
        }
    }

    private suspend fun getAccounts() =
            caldavDao.getAccounts(TYPE_CALDAV, TYPE_TASKS)
}