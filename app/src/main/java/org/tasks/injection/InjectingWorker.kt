package org.tasks.injection

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.tasks.analytics.Firebase
import timber.log.Timber

abstract class InjectingWorker(
        internal val context: Context,
        workerParams: WorkerParameters,
        internal val firebase: Firebase) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Timber.d("%s.doWork()", javaClass.simpleName)
        return try {
            run()
        } catch (e: Exception) {
            firebase.reportException(e)
            Result.failure()
        }
    }

    protected abstract fun run(): Result
}