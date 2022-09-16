package org.tasks.jobs

import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.net.ConnectivityManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.caldav.CaldavSynchronizer
import org.tasks.data.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskListDao
import org.tasks.data.OpenTaskDao
import org.tasks.etebase.EtebaseSynchronizer
import org.tasks.gtasks.GoogleTaskSynchronizer
import org.tasks.injection.BaseWorker
import org.tasks.opentasks.OpenTasksSynchronizer
import org.tasks.preferences.Preferences

@HiltWorker
class SyncWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val localBroadcastManager: LocalBroadcastManager,
    private val preferences: Preferences,
    private val caldavDao: CaldavDao,
    private val caldavSynchronizer: Lazy<CaldavSynchronizer>,
    private val etebaseSynchronizer: Lazy<EtebaseSynchronizer>,
    private val googleTaskSynchronizer: Lazy<GoogleTaskSynchronizer>,
    private val openTasksSynchronizer: Lazy<OpenTasksSynchronizer>,
    private val googleTaskListDao: GoogleTaskListDao,
    private val openTaskDao: OpenTaskDao,
    private val inventory: Inventory
) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        if (isBackground) {
            ContextCompat.getSystemService(context, ConnectivityManager::class.java)?.apply {
                if (restrictBackgroundStatus == ConnectivityManagerCompat.RESTRICT_BACKGROUND_STATUS_ENABLED) {
                    return Result.failure()
                }
            }
        }

        synchronized(LOCK) {
            if (preferences.getBoolean(syncStatus, false)) {
                return Result.retry()
            }
            preferences.setBoolean(syncStatus, true)
        }
        localBroadcastManager.broadcastRefresh()
        try {
            doSync()
        } catch (e: Exception) {
            firebase.reportException(e)
        } finally {
            preferences.setBoolean(syncStatus, false)
            localBroadcastManager.broadcastRefresh()
        }
        return Result.success()
    }

    private val isImmediate: Boolean
        get() = inputData.getBoolean(EXTRA_IMMEDIATE, false)

    private val isBackground: Boolean
        get() = inputData.getBoolean(EXTRA_BACKGROUND, false)

    private val syncStatus = R.string.p_sync_ongoing

    private suspend fun doSync() {
        if (preferences.isManualSort) {
            preferences.isPositionHackEnabled = true
        }
        googleTaskJobs().plus(caldavJobs()).awaitAll()
        inventory.updateTasksAccount()
        if (openTaskDao.shouldSync()) {
            openTasksSynchronizer.get().sync()

            if (isImmediate) {
                AccountManager
                    .get(context)
                    .accounts
                    .filter { OpenTaskDao.SUPPORTED_TYPES.contains(it.type) }
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

    private suspend fun googleTaskJobs(): List<Deferred<Unit>> = coroutineScope {
        getGoogleAccounts()
            .mapIndexed { i, account ->
                async(Dispatchers.IO) {
                    googleTaskSynchronizer.get().sync(account, i)
                }
            }
    }

    private suspend fun caldavJobs(): List<Deferred<Unit>> = coroutineScope {
        getCaldavAccounts().map {
            async(Dispatchers.IO) {
                when (it.accountType) {
                    TYPE_ETEBASE -> etebaseSynchronizer.get().sync(it)
                    TYPE_TASKS,
                    TYPE_CALDAV -> caldavSynchronizer.get().sync(it)
                }
            }
        }
    }

    private suspend fun getGoogleAccounts() =
        googleTaskListDao.getAccounts()

    private suspend fun getCaldavAccounts() =
            caldavDao.getAccounts(TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE)

    companion object {
        private val LOCK = Any()

        const val EXTRA_IMMEDIATE = "extra_immediate"
        const val EXTRA_BACKGROUND = "extra_background"
    }
}