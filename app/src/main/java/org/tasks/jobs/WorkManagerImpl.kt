package org.tasks.jobs

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.*
import androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE
import androidx.work.ExistingWorkPolicy.REPLACE
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.AndroidUtilities.atLeastS
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.*
import org.tasks.data.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.date.DateTimeUtils.midnight
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.jobs.DriveUploader.Companion.EXTRA_PURGE
import org.tasks.jobs.DriveUploader.Companion.EXTRA_URI
import org.tasks.jobs.MigrateLocalWork.Companion.EXTRA_ACCOUNT
import org.tasks.jobs.SyncWork.Companion.EXTRA_BACKGROUND
import org.tasks.jobs.SyncWork.Companion.EXTRA_IMMEDIATE
import org.tasks.jobs.WorkManager.Companion.REMOTE_CONFIG_INTERVAL_HOURS
import org.tasks.jobs.WorkManager.Companion.TAG_BACKGROUND_SYNC
import org.tasks.jobs.WorkManager.Companion.TAG_BACKUP
import org.tasks.jobs.WorkManager.Companion.TAG_MIDNIGHT_REFRESH
import org.tasks.jobs.WorkManager.Companion.TAG_MIGRATE_LOCAL
import org.tasks.jobs.WorkManager.Companion.TAG_REFRESH
import org.tasks.jobs.WorkManager.Companion.TAG_REMOTE_CONFIG
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC
import org.tasks.jobs.WorkManager.Companion.TAG_UPDATE_PURCHASES
import org.tasks.notifications.Throttle
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class WorkManagerImpl(
        private val context: Context,
        private val preferences: Preferences,
        private val caldavDao: CaldavDao,
        private val openTaskDao: OpenTaskDao
): WorkManager {
    private val throttle = Throttle(200, 60000, "WORK")
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val workManager = androidx.work.WorkManager.getInstance(context)

    override fun updateCalendar(task: Task) {
        enqueue(
            OneTimeWorkRequest.Builder(UpdateCalendarWork::class.java)
                .setInputData(UpdateCalendarWork.EXTRA_ID to task.id)
        )
    }

    @SuppressLint("EnqueueWork")
    override fun migrateLocalTasks(caldavAccount: CaldavAccount) {
        val builder = OneTimeWorkRequest.Builder(MigrateLocalWork::class.java)
                .setInputData(EXTRA_ACCOUNT to caldavAccount.uuid)
                .setConstraints(networkConstraints)
        enqueue(workManager.beginUniqueWork(TAG_MIGRATE_LOCAL, APPEND_OR_REPLACE, builder.build()))
    }

    override suspend fun startEnqueuedSync() {
        if (getSyncJob().any { it.state == WorkInfo.State.ENQUEUED }) {
            sync(true)
        }
    }

    @SuppressLint("EnqueueWork")
    override suspend fun sync(immediate: Boolean) {
        val builder = OneTimeWorkRequest.Builder(SyncWork::class.java)
                .setInputData(EXTRA_IMMEDIATE to immediate)
        if (!openTaskDao.shouldSync()) {
            builder.setConstraints(networkConstraints)
        }
        if (!immediate) {
            builder.setInitialDelay(1, TimeUnit.MINUTES)
        }
        val append = getSyncJob().any { it.state == WorkInfo.State.RUNNING }
        Timber.d("sync: immediate=$immediate, append=$append)")
        enqueue(workManager.beginUniqueWork(
                TAG_SYNC,
                if (append) APPEND_OR_REPLACE else REPLACE,
                builder.build())
        )
    }

    override fun reverseGeocode(place: Place) {
        if (BuildConfig.DEBUG && place.id == 0L) {
            throw RuntimeException("Missing id")
        }
        enqueue(
                OneTimeWorkRequest.Builder(ReverseGeocodeWork::class.java)
                        .setInputData(ReverseGeocodeWork.PLACE_ID to place.id)
                        .setConstraints(networkConstraints))
    }

    override fun updateBackgroundSync() {
        throttle.run {
            val enabled = caldavDao.getAccounts(TYPE_GOOGLE_TASKS, TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE).isNotEmpty()
            if (enabled) {
                Timber.d("Enabling background sync")
                val builder = PeriodicWorkRequest.Builder(SyncWork::class.java, 1, TimeUnit.HOURS)
                    .setInputData(EXTRA_BACKGROUND to true)
                    .setConstraints(networkConstraints)
                workManager.enqueueUniquePeriodicWork(
                    TAG_BACKGROUND_SYNC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    builder.build()
                )
            } else {
                Timber.d("Disabling background sync")
                workManager.cancelUniqueWork(TAG_BACKGROUND_SYNC)
            }
        }
    }

    override fun scheduleRefresh(time: Long) = enqueueUnique(TAG_REFRESH, RefreshWork::class.java, time)

    override fun scheduleMidnightRefresh() =
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
            if (!atLeastS() || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            }
        }
    }

    override fun scheduleBackup() =
            enqueueUnique(
                    TAG_BACKUP,
                    BackupWork::class.java,
                    newDateTime(preferences.getLong(R.string.p_last_backup, 0L))
                            .plusDays(1)
                            .millis
                            .coerceAtMost(midnight()))

    override fun scheduleConfigRefresh() {
        throttle.run {
            workManager.enqueueUniquePeriodicWork(
                    TAG_REMOTE_CONFIG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequest.Builder(
                            RemoteConfigWork::class.java, REMOTE_CONFIG_INTERVAL_HOURS, TimeUnit.HOURS)
                            .setConstraints(networkConstraints)
                            .build())
        }
    }

    override fun scheduleDriveUpload(uri: Uri, purge: Boolean) {
        if (!preferences.getBoolean(R.string.p_google_drive_backup, false)) {
            return
        }
        val builder = OneTimeWorkRequest.Builder(DriveUploader::class.java)
            .setInputData(
                EXTRA_URI to uri.toString(),
                EXTRA_PURGE to purge,
            )
            .setConstraints(networkConstraints)
        if (purge) {
            builder.setInitialDelay(Random().nextInt(3600).toLong(), TimeUnit.SECONDS)
        }
        enqueue(builder)
    }

    private val networkConstraints: Constraints
        get() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    override fun cancelNotifications() {
        alarmManager.cancel(notificationPendingIntent)
    }

    override fun updatePurchases() =
        enqueueUnique(TAG_UPDATE_PURCHASES, UpdatePurchaseWork::class.java)

    @SuppressLint("EnqueueWork")
    private fun enqueueUnique(key: String, c: Class<out Worker?>, time: Long = 0) {
        val delay = time - DateUtilities.now()
        val builder = OneTimeWorkRequest.Builder(c)
        if (delay > 0) {
            builder.setInitialDelay(delay, TimeUnit.MILLISECONDS)
        }
        Timber.d("$key: ${DateTimeUtils.printTimestamp(time)} (${DateTimeUtils.printDuration(delay)})")
        enqueue(workManager.beginUniqueWork(key, REPLACE, builder.build()))
    }

    private fun enqueue(builder: WorkRequest.Builder<*, *>) {
        throttle.run {
            workManager.enqueue(builder.build())
        }
    }

    private fun enqueue(continuation: WorkContinuation) {
        throttle.run {
            continuation.enqueue()
        }
    }

    private val notificationIntent: Intent
        get() = Intent(context, NotificationService::class.java)

    private val notificationPendingIntent: PendingIntent
        get() {
            return if (AndroidUtilities.atLeastOreo()) {
                PendingIntent.getForegroundService(
                    context,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                PendingIntent.getService(
                    context,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        }

    private suspend fun getSyncJob() = withContext(Dispatchers.IO) {
        workManager.getWorkInfosForUniqueWork(TAG_SYNC).get()
    }
}

private fun <B : WorkRequest.Builder<B, *>, W : WorkRequest> WorkRequest.Builder<B, W>.setInputData(
    vararg pairs: Pair<String, Any?>
): B = setInputData(workDataOf(*pairs))
