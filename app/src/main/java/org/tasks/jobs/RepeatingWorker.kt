package org.tasks.jobs

import android.content.Context
import androidx.work.WorkerParameters
import org.tasks.analytics.Firebase
import org.tasks.injection.BaseWorker

abstract class RepeatingWorker internal constructor(
        context: Context,
        workerParams: WorkerParameters,
        firebase: Firebase) : BaseWorker(context, workerParams, firebase) {

    override suspend fun doWork(): Result {
        val result = super.doWork()
        scheduleNext()
        return result
    }

    protected abstract fun scheduleNext()
}