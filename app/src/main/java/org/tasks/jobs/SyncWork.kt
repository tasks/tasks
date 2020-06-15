package org.tasks.jobs

import android.content.Context
import androidx.work.WorkerParameters
import org.tasks.LocalBroadcastManager
import org.tasks.caldav.CaldavSynchronizer
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskListDao
import org.tasks.etesync.EteSynchronizer
import org.tasks.gtasks.GoogleTaskSynchronizer
import org.tasks.injection.ApplicationComponent
import org.tasks.injection.InjectingWorker
import org.tasks.preferences.Preferences
import org.tasks.sync.SyncAdapters
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncWork(context: Context, workerParams: WorkerParameters) : InjectingWorker(context, workerParams) {
    @Inject lateinit var caldavSynchronizer: CaldavSynchronizer
    @Inject lateinit var eteSynchronizer: EteSynchronizer
    @Inject lateinit var googleTaskSynchronizer: GoogleTaskSynchronizer
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var googleTaskListDao: GoogleTaskListDao
    @Inject lateinit var syncAdapters: SyncAdapters
    
    public override fun run(): Result {
        if (!syncAdapters.isSyncEnabled) {
            return Result.success()
        }
        synchronized(LOCK) {
            if (preferences.isSyncOngoing) {
                return Result.retry()
            }
        }
        preferences.isSyncOngoing = true
        localBroadcastManager.broadcastRefresh()
        try {
            sync()
        } catch (e: Exception) {
            firebase.reportException(e)
        } finally {
            preferences.isSyncOngoing = false
            localBroadcastManager.broadcastRefresh()
        }
        return Result.success()
    }

    @Throws(InterruptedException::class)
    private fun sync() {
        val numThreads = Runtime.getRuntime().availableProcessors()
        val executor = Executors.newFixedThreadPool(numThreads)
        for (account in caldavDao.getAccounts()) {
            executor.execute {
                if (account.isCaldavAccount) {
                    caldavSynchronizer.sync(account)
                } else if (account.isEteSyncAccount) {
                    eteSynchronizer.sync(account)
                }
            }
        }
        val accounts = googleTaskListDao.getAccounts()
        for (i in accounts.indices) {
            executor.execute { googleTaskSynchronizer.sync(accounts[i], i) }
        }
        executor.shutdown()
        executor.awaitTermination(15, TimeUnit.MINUTES)
    }

    override fun inject(component: ApplicationComponent) = component.inject(this)

    companion object {
        private val LOCK = Any()
    }
}