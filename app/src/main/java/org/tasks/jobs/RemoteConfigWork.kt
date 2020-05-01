package org.tasks.jobs

import android.content.Context
import androidx.work.WorkerParameters
import org.tasks.injection.InjectingWorker
import org.tasks.injection.JobComponent

class RemoteConfigWork(context: Context, workerParams: WorkerParameters) : InjectingWorker(context, workerParams) {
    override fun run(): Result {
        firebase.updateRemoteConfig()
        return Result.success()
    }

    override fun inject(component: JobComponent) = component.inject(this)
}