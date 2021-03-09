package org.tasks.jobs

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.ConnectivityManagerCompat.RESTRICT_BACKGROUND_STATUS_ENABLED
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
        private val preferences: Preferences
) : BaseWorker(context, workerParams, firebase) {

    final override suspend fun run(): Result {
        if (!enabled()) {
            return Result.failure()
        }
        if (isBackground) {
            getSystemService(context, ConnectivityManager::class.java)?.apply {
                if (restrictBackgroundStatus == RESTRICT_BACKGROUND_STATUS_ENABLED) {
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

    val isImmediate: Boolean
        get() = inputData.getBoolean(EXTRA_IMMEDIATE, false)

    private val isBackground: Boolean
        get() = inputData.getBoolean(EXTRA_BACKGROUND, false)

    protected abstract val syncStatus: Int

    protected abstract suspend fun enabled(): Boolean

    protected abstract suspend fun doSync()

    companion object {
        private val LOCK = Any()

        const val EXTRA_IMMEDIATE = "extra_immediate"
        const val EXTRA_BACKGROUND = "extra_background"
    }
}