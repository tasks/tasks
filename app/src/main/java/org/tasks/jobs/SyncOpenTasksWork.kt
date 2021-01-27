package org.tasks.jobs

import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.data.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.CaldavDao
import org.tasks.data.OpenTaskDao
import org.tasks.data.OpenTaskDao.Companion.SUPPORTED_TYPES
import org.tasks.opentasks.OpenTasksSynchronizer
import org.tasks.preferences.Preferences

@HiltWorker
class SyncOpenTasksWork @AssistedInject constructor(
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

        if (isImmediate) {
            AccountManager
                    .get(context)
                    .accounts
                    .filter { SUPPORTED_TYPES.contains(it.type) }
                    .forEach {
                        ContentResolver.requestSync(
                                it,
                                openTaskDao.authority,
                                Bundle().apply {
                                    putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                                    putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                                }
                        )
                    }
        }
    }
}