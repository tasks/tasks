package org.tasks.injection

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.todoroo.andlib.utility.AndroidUtilities.atLeastAndroid16
import kotlinx.coroutines.runBlocking
import org.tasks.analytics.Firebase
import timber.log.Timber

abstract class BaseWorker(
        internal val context: Context,
        workerParams: WorkerParameters,
        internal val firebase: Firebase
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        if (atLeastAndroid16()) {
            Timber.d("${javaClass.simpleName} $id $inputData attempt=$runAttemptCount ${if (runAttemptCount > 0) "stopReason=$stopReason" else ""}")
        } else {
            Timber.d("${javaClass.simpleName} $id $inputData attempt=$runAttemptCount")
        }
        return try {
            runBlocking {
                run()
            }
        } catch (e: Exception) {
            firebase.reportException(e)
            Result.failure()
        }
    }

    protected abstract suspend fun run(): Result
}