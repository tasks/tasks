package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.LocalBroadcastManager
import org.tasks.analytics.Firebase

@HiltWorker
class MidnightRefreshWork @AssistedInject constructor(
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