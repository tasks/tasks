package org.tasks.jobs

import android.content.Context
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import org.tasks.analytics.Firebase
import org.tasks.injection.BaseWorker

abstract class RepeatingWorker internal constructor(
        context: Context,
        workerParams: WorkerParameters,
        firebase: Firebase
) : BaseWorker(context, workerParams, firebase) {

    override fun doWork(): Result {
        val result = super.doWork()
        runBlocking {
            scheduleNext()
        }
        return result
    }

    protected abstract suspend fun scheduleNext()
}