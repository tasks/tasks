package org.tasks.jobs

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import org.tasks.LocalBroadcastManager
import org.tasks.analytics.Firebase

class MidnightRefreshWork @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        private val workManager: WorkManager,
        private val localBroadcastManager: LocalBroadcastManager) : RepeatingWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        localBroadcastManager.broadcastRefresh()
        return Result.success()
    }

    override suspend fun scheduleNext() = workManager.scheduleMidnightRefresh()
}