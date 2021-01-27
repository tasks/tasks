package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.analytics.Firebase
import org.tasks.injection.BaseWorker

@HiltWorker
class RemoteConfigWork @AssistedInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        firebase.updateRemoteConfig()
        return Result.success()
    }
}