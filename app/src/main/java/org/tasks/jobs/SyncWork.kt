package org.tasks.jobs

import android.content.Context
import androidx.work.WorkerParameters
import org.tasks.LocalBroadcastManager
import org.tasks.analytics.Firebase
import org.tasks.injection.BaseWorker
import org.tasks.preferences.Preferences

abstract class SyncWork constructor(
        context: Context,
        workerParams: WorkerParameters,
        firebase: Firebase,
        private val localBroadcastManager: LocalBroadcastManager,
        private val preferences: Preferences) : BaseWorker(context, workerParams, firebase) {
    
    final override suspend fun run(): Result {
        if (!enabled()) {
            return Result.failure()
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

    protected abstract val syncStatus: Int

    protected abstract suspend fun enabled(): Boolean

    protected abstract suspend fun doSync()

    companion object {
        private val LOCK = Any()
    }
}