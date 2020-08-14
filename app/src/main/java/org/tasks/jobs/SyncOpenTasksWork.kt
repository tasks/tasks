package org.tasks.jobs

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.data.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.CaldavDao
import org.tasks.data.OpenTaskDao
import org.tasks.opentasks.OpenTasksSynchronizer
import org.tasks.preferences.Preferences

class SyncOpenTasksWork @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        localBroadcastManager: LocalBroadcastManager,
        preferences: Preferences,
        private val openTasksSynchronizer: OpenTasksSynchronizer,
        private val caldavDao: CaldavDao,
        private val openTaskDao: OpenTaskDao
) : SyncWork(context, workerParams, firebase, localBroadcastManager, preferences) {
    override val syncStatus = R.string.p_sync_ongoing_opentasks

    override suspend fun enabled() =
            caldavDao.getAccounts(TYPE_OPENTASKS).isNotEmpty()
                    || openTaskDao.newAccounts().isNotEmpty()

    override suspend fun doSync() {
        openTasksSynchronizer.sync()
    }
}