package org.tasks.jobs

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.*
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskListDao
import org.tasks.data.Place
import org.tasks.date.DateTimeUtils.midnight
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.db.SuspendDbUtils.eachChunk
import org.tasks.jobs.WorkManager.Companion.MAX_CLEANUP_LENGTH
import org.tasks.jobs.WorkManager.Companion.REMOTE_CONFIG_INTERVAL_HOURS
import org.tasks.jobs.WorkManager.Companion.TAG_BACKGROUND_SYNC
import org.tasks.jobs.WorkManager.Companion.TAG_BACKUP
import org.tasks.jobs.WorkManager.Companion.TAG_MIDNIGHT_REFRESH
import org.tasks.jobs.WorkManager.Companion.TAG_REFRESH
import org.tasks.jobs.WorkManager.Companion.TAG_REMOTE_CONFIG
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC
import org.tasks.notifications.Throttle
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class WorkManagerImpl constructor(
        private val context: Context,
        private val preferences: Preferences,
        private val googleTaskListDao: GoogleTaskListDao,
        private val caldavDao: CaldavDao): WorkManager {
    private val throttle = Throttle(200, 60000, "WORK")
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val workManager = androidx.work.WorkManager.getInstance(context)

    private suspend fun enqueue(builder: WorkRequest.Builder<*, *>) {
        throttle.run {
            workManager.enqueue(builder.build())
        }
    }

    private suspend fun enqueue(continuation: WorkContinuation) {
        throttle.run {
            continuation.enqueue()
        }
    }

    override suspend fun afterComplete(task: Task) {
        enqueue(
                OneTimeWorkRequest.Builder(AfterSaveWork::class.java)
                        .setInputData(Data.Builder()
                                .putLong(AfterSaveWork.EXTRA_ID, task.id)
                                .build()))
    }

    override suspend fun cleanup(ids: Iterable<Long>) {
        ids.eachChunk(MAX_CLEANUP_LENGTH) {
            enqueue(
                    OneTimeWorkRequest.Builder(CleanupWork::class.java)
                            .setInputData(
                                    Data.Builder()
                                            .putLongArray(CleanupWork.EXTRA_TASK_IDS, it.toLongArray())
                                            .build()))
        }
    }

    override suspend fun sync(immediate: Boolean) {
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                        if (!immediate && preferences.getBoolean(R.string.p_background_sync_unmetered_only, false)) {
                            NetworkType.UNMETERED
                        } else {
                            NetworkType.CONNECTED
                        })
                .build()
        val builder = OneTimeWorkRequest.Builder(SyncWork::class.java)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setConstraints(constraints)
        if (!immediate) {
            builder.setInitialDelay(1, TimeUnit.MINUTES)
        }
        throttle.run {
            workManager
                    .beginUniqueWork(TAG_SYNC, ExistingWorkPolicy.REPLACE, builder.build())
                    .enqueue()
        }
    }

    override suspend fun reverseGeocode(place: Place) {
        if (BuildConfig.DEBUG && place.id == 0L) {
            throw RuntimeException("Missing id")
        }
        enqueue(
                OneTimeWorkRequest.Builder(ReverseGeocodeWork::class.java)
                        .setInputData(Data.Builder().putLong(ReverseGeocodeWork.PLACE_ID, place.id).build())
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                        .setConstraints(
                                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()))
    }

    override suspend fun updateBackgroundSync() {
        updateBackgroundSync(null, null, null)
    }

    @SuppressLint("CheckResult")
    override suspend fun updateBackgroundSync(
            forceAccountPresent: Boolean?,
            forceBackgroundEnabled: Boolean?,
            forceOnlyOnUnmetered: Boolean?) {
        val backgroundEnabled = forceBackgroundEnabled
                ?: preferences.getBoolean(R.string.p_background_sync, true)
        val onlyOnWifi = forceOnlyOnUnmetered
                ?: preferences.getBoolean(R.string.p_background_sync_unmetered_only, false)
        val accountsPresent = forceAccountPresent == true
                || googleTaskListDao.accountCount() > 0
                || caldavDao.accountCount() > 0
        scheduleBackgroundSync(backgroundEnabled && accountsPresent, onlyOnWifi)
    }

    private suspend fun scheduleBackgroundSync(enabled: Boolean, onlyOnUnmetered: Boolean) {
        Timber.d("background sync enabled: %s, onlyOnUnmetered: %s", enabled, onlyOnUnmetered)
        throttle.run {
            if (enabled) {
                workManager.enqueueUniquePeriodicWork(
                        TAG_BACKGROUND_SYNC,
                        ExistingPeriodicWorkPolicy.KEEP,
                        PeriodicWorkRequest.Builder(SyncWork::class.java, 1, TimeUnit.HOURS)
                                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                                .setConstraints(getNetworkConstraints(onlyOnUnmetered))
                                .build())
            } else {
                workManager.cancelUniqueWork(TAG_BACKGROUND_SYNC)
            }
        }
    }

    override suspend fun scheduleRefresh(time: Long) = enqueueUnique(TAG_REFRESH, RefreshWork::class.java, time)

    override suspend fun scheduleMidnightRefresh() =
            enqueueUnique(TAG_MIDNIGHT_REFRESH, MidnightRefreshWork::class.java, midnight())

    override fun scheduleNotification(scheduledTime: Long) {
        val time = max(DateUtilities.now(), scheduledTime)
        if (time < DateTimeUtils.currentTimeMillis()) {
            val intent = notificationIntent
            if (AndroidUtilities.atLeastOreo()) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            val pendingIntent = notificationPendingIntent
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }
    }

    override suspend fun scheduleBackup() =
            enqueueUnique(
                    TAG_BACKUP,
                    BackupWork::class.java,
                    newDateTime(preferences.getLong(R.string.p_last_backup, 0L))
                            .plusDays(1)
                            .millis
                            .coerceAtMost(midnight()))

    override suspend fun scheduleConfigRefresh() {
        throttle.run {
            workManager.enqueueUniquePeriodicWork(
                    TAG_REMOTE_CONFIG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequest.Builder(
                            RemoteConfigWork::class.java, REMOTE_CONFIG_INTERVAL_HOURS, TimeUnit.HOURS)
                            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                            .setConstraints(
                                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                            .build())
        }
    }

    override suspend fun scheduleDriveUpload(uri: Uri, purge: Boolean) {
        if (!preferences.getBoolean(R.string.p_google_drive_backup, false)) {
            return
        }
        val builder = OneTimeWorkRequest.Builder(DriveUploader::class.java)
                .setInputData(DriveUploader.getInputData(uri, purge))
                .setConstraints(networkConstraints)
        if (purge) {
            builder.setInitialDelay(Random().nextInt(3600).toLong(), TimeUnit.SECONDS)
        }
        enqueue(builder)
    }

    private val networkConstraints: Constraints
        get() = getNetworkConstraints(
                preferences.getBoolean(R.string.p_background_sync_unmetered_only, false))

    private fun getNetworkConstraints(unmeteredOnly: Boolean) =
            Constraints.Builder()
                    .setRequiredNetworkType(if (unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .build()

    @SuppressLint("EnqueueWork")
    private suspend fun enqueueUnique(key: String, c: Class<out CoroutineWorker?>, time: Long) {
        val delay = time - DateUtilities.now()
        val builder = OneTimeWorkRequest.Builder(c)
        if (delay > 0) {
            builder.setInitialDelay(delay, TimeUnit.MILLISECONDS)
        }
        Timber.d("$key: ${DateTimeUtils.printTimestamp(time)} (${DateTimeUtils.printDuration(delay)})")
        enqueue(workManager.beginUniqueWork(key, ExistingWorkPolicy.REPLACE, builder.build()))
    }

    override fun cancelNotifications() {
        Timber.d("cancelNotifications")
        alarmManager.cancel(notificationPendingIntent)
    }

    private val notificationIntent: Intent
        get() = Intent(context, NotificationService::class.java)

    private val notificationPendingIntent: PendingIntent
        get() {
            return if (AndroidUtilities.atLeastOreo()) {
                PendingIntent.getForegroundService(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            } else {
                PendingIntent.getService(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
        }
}