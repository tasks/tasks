package org.tasks.jobs

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkContinuation
import androidx.work.WorkInfo
import androidx.work.WorkRequest
import androidx.work.Worker
import androidx.work.workDataOf
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.AndroidUtilities.atLeastS
import org.tasks.data.entity.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.dao.CaldavDao
import org.tasks.data.OpenTaskDao
import org.tasks.data.entity.Place
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
import org.tasks.jobs.WorkManager.Companion.TAG_MIGRATE_LOCAL
import org.tasks.jobs.WorkManager.Companion.TAG_NOTIFICATIONS
import org.tasks.jobs.WorkManager.Companion.TAG_REFRESH
import org.tasks.jobs.WorkManager.Companion.TAG_REMOTE_CONFIG
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC
import org.tasks.jobs.WorkManager.Companion.TAG_UPDATE_PURCHASES
import org.tasks.notifications.Throttle
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.printTimestamp
import timber.log.Timber
import java.util.Random
import java.util.concurrent.TimeUnit
import kotlin.math.max

class WorkManagerImpl(
    private val context: Context,
    private val preferences: Preferences,
    private val caldavDao: CaldavDao,
    private val openTaskDao: OpenTaskDao,
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

    override suspend fun scheduleRefresh(timestamp: Long) =
        enqueueUnique(TAG_REFRESH, RefreshWork::class.java, timestamp)

    override fun triggerNotifications(expedited: Boolean) {
        enqueueUnique(
            TAG_NOTIFICATIONS,
            NotificationWork::class.java,
            time = if (expedited) 0 else currentTimeMillis() + 5_000,
            expedited = expedited,
        )
    }

    override fun scheduleNotification(scheduledTime: Long) {
        val time = max(currentTimeMillis(), scheduledTime)
        if (time < currentTimeMillis()) {

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

    override fun updatePurchases() =
        enqueueUnique(TAG_UPDATE_PURCHASES, UpdatePurchaseWork::class.java)

    @SuppressLint("EnqueueWork")
    private fun enqueueUnique(
        key: String,
        c: Class<out Worker?>,
        time: Long = 0,
        expedited: Boolean = false,
    ) {
        val delay = time - currentTimeMillis()
        val builder = OneTimeWorkRequest.Builder(c)
        if (delay > 0) {
            builder.setInitialDelay(delay, TimeUnit.MILLISECONDS)
        }
        if (expedited) {
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }
        Timber.d("$key: expedited=$expedited ${printTimestamp(delay)} (${DateTimeUtils.printDuration(delay)})")
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
        get() = Intent(context, NotificationReceiver::class.java)

    private val notificationPendingIntent: PendingIntent
        get() = PendingIntent.getBroadcast(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private suspend fun getSyncJob() = withContext(Dispatchers.IO) {
        workManager.getWorkInfosForUniqueWork(TAG_SYNC).get()
    }
}

private fun <B : WorkRequest.Builder<B, *>, W : WorkRequest> WorkRequest.Builder<B, W>.setInputData(
    vararg pairs: Pair<String, Any?>
): B = setInputData(workDataOf(*pairs))
