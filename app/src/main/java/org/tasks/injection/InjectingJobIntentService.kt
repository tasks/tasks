package org.tasks.injection

import android.content.Intent
import androidx.core.app.JobIntentService
import timber.log.Timber

abstract class InjectingJobIntentService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        try {
            doWork(intent)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    protected abstract fun doWork(intent: Intent)

    companion object {
        const val JOB_ID_GEOFENCE_TRANSITION = 1081
        const val JOB_ID_REFRESH_RECEIVER = 1082
        const val JOB_ID_NOTIFICATION_SCHEDULER = 1084
        const val JOB_ID_CALENDAR_NOTIFICATION = 1085
        const val JOB_ID_TASKER = 1086
    }
}