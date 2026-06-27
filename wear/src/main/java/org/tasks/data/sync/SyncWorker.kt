/**
 * SyncWorker.kt — Periodic WorkManager worker for background sync.
 *
 * Scheduled every 15 minutes (with `NetworkType.CONNECTED` constraint)
 * via [SyncWorker.schedule].  Each run:
 *
 * 1. Resets stuck outbox operations that stayed in `SENDING` too long.
 * 2. Sends pending outbox operations via [DataLayerSyncManager].
 * 3. Cleans up acknowledged outbox rows.
 * 4. Purges old processed-op idempotency records.
 * 5. Removes soft-deleted tasks that have been synced.
 * 6. Reschedules reminder alarms for newly synced tasks.
 *
 * Also provides [syncNow] for on-demand immediate sync.
 */
package org.tasks.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.tasks.notifications.WearNotificationManager
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Worker that periodically processes pending sync operations.
 *
 * This handles:
 * - Retrying failed operations
 * - Cleaning up acknowledged operations
 * - Resetting stuck operations
 * - Requesting sync from phone if needed
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val syncManager = DataLayerSyncManager.getInstance(applicationContext)
    private val syncRepository = SyncRepository.getInstance(applicationContext)

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker starting")

        return try {
            // Reset stuck operations (sending for too long)
            syncRepository.resetStuckOps()

            // Process pending operations
            syncManager.processPendingOps()

            // Clean up acknowledged operations
            syncRepository.cleanupAckedOps()

            // Clean up old processed ops
            syncRepository.cleanupOldProcessedOps()

            // Clean up deleted tasks that have been synced
            syncRepository.cleanupDeletedTasks()

            // Reschedule reminders for any newly synced tasks
            WearNotificationManager.getInstance(applicationContext).rescheduleAll()

            Timber.d("SyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker failed")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "sync_worker"
        private const val SYNC_INTERVAL_MINUTES = 15L

        /**
         * Schedule periodic sync work.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

            Timber.d("Scheduled periodic sync every $SYNC_INTERVAL_MINUTES minutes")
        }

        /**
         * Cancel scheduled sync work.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("Cancelled periodic sync")
        }

        /**
         * Trigger an immediate sync.
         */
        suspend fun syncNow(context: Context) {
            val syncManager = DataLayerSyncManager.getInstance(context)
            val syncRepository = SyncRepository.getInstance(context)

            syncRepository.resetStuckOps()
            syncManager.processPendingOps()
            syncRepository.cleanupAckedOps()
        }
    }
}
