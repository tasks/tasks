package org.tasks.jobs

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import org.tasks.analytics.Firebase
import org.tasks.injection.BaseWorker

class RemoteConfigWork @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        firebase.updateRemoteConfig()
        return Result.success()
    }
}