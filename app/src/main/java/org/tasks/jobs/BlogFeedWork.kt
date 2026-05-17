package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import org.tasks.analytics.Firebase
import org.tasks.feed.BlogFeedChecker
import org.tasks.injection.BaseWorker
import timber.log.Timber

@HiltWorker
class BlogFeedWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val blogFeedChecker: BlogFeedChecker,
    private val workManager: WorkManager,
) : BaseWorker(context, workerParams, firebase) {

    override fun doWork(): Result {
        val result = super.doWork()
        if (result != Result.retry()) {
            runBlocking { workManager.scheduleBlogFeedCheck() }
        }
        return result
    }

    override suspend fun run(): Result {
        val result = try {
            blogFeedChecker.check()
        } catch (e: Exception) {
            firebase.reportException(e)
            WorkResult.Fail
        }
        Timber.d("BlogFeedWork: $result")
        return if (result == WorkResult.Fail) Result.retry() else Result.success()
    }
}
