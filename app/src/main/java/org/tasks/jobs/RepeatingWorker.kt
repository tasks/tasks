package org.tasks.jobs

import android.content.Context
import androidx.work.WorkerParameters
import org.tasks.analytics.Firebase
import org.tasks.injection.InjectingWorker

abstract class RepeatingWorker internal constructor(
        context: Context,
        workerParams: WorkerParameters,
        firebase: Firebase) : InjectingWorker(context, workerParams, firebase) {

    override fun doWork(): Result {
        val result = super.doWork()
        scheduleNext()
        return result
    }

    protected abstract fun scheduleNext()
}