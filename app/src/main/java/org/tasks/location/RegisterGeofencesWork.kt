package org.tasks.location

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.analytics.Firebase
import org.tasks.injection.BaseWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class RegisterGeofencesWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val locationService: LocationService,
) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        return try {
            locationService.registerAllGeofences()
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Failed to register geofences")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "register_geofences"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<RegisterGeofencesWork>()
                        .setInitialDelay(60, TimeUnit.SECONDS)
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            1,
                            TimeUnit.MINUTES,
                        )
                        .build()
                )
        }
    }
}
