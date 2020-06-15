package org.tasks.jobs

import android.content.Context
import androidx.work.WorkerParameters
import org.tasks.injection.InjectingWorker

abstract class RepeatingWorker internal constructor(context: Context, workerParams: WorkerParameters) : InjectingWorker(context, workerParams) {
    override fun doWork(): Result {
        val result = super.doWork()
        scheduleNext()
        return result
    }

    protected abstract fun scheduleNext()
}