package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.LocalBroadcastManager
import org.tasks.analytics.Firebase
import org.tasks.scheduling.RefreshScheduler

@HiltWorker
class RefreshWork @AssistedInject constructor(
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