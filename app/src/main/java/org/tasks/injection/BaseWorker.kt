package org.tasks.injection

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.tasks.analytics.Firebase
import timber.log.Timber

abstract class BaseWorker(
        internal val context: Context,
        workerParams: WorkerParameters,
        internal val firebase: Firebase) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("%s.doWork()", javaClass.simpleName)
        return try {
            run()
        } catch (e: Exception) {
            firebase.reportException(e)
            Result.failure()
        }
    }

    protected abstract suspend fun run(): Result
}