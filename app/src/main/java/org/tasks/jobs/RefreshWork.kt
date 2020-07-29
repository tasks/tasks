package org.tasks.jobs

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import org.tasks.LocalBroadcastManager
import org.tasks.analytics.Firebase
import org.tasks.scheduling.RefreshScheduler

class RefreshWork @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        private val refreshScheduler: RefreshScheduler,
        private val localBroadcastManager: LocalBroadcastManager) : RepeatingWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        localBroadcastManager.broadcastRefresh()
        return Result.success()
    }

    override suspend fun scheduleNext() = refreshScheduler.scheduleNext()
}