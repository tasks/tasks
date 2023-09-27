package org.tasks.injection

import android.content.Intent
import androidx.core.app.JobIntentService
import kotlinx.coroutines.runBlocking
import timber.log.Timber

abstract class InjectingJobIntentService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        runBlocking {
            try {
                doWork(intent)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    protected abstract suspend fun doWork(intent: Intent)

    companion object {
        const val JOB_ID_GEOFENCE_TRANSITION = 1081
        const val JOB_ID_REFRESH_RECEIVER = 1082
        const val JOB_ID_NOTIFICATION_SCHEDULER = 1084
        const val JOB_ID_TASKER = 1086
    }
}