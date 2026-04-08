package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.analytics.Firebase
import org.tasks.feed.BlogFeedChecker
import timber.log.Timber

@HiltWorker
class BlogFeedWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val blogFeedChecker: BlogFeedChecker,
    private val workManager: WorkManager,
) : RepeatingWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        val result = try {
            blogFeedChecker.check()
        } catch (e: Exception) {
            firebase.reportException(e)
            WorkResult.Fail
        }
        Timber.d("BlogFeedWork: $result")
        return Result.success()
    }

    override suspend fun scheduleNext() = workManager.scheduleBlogFeedCheck()
}
