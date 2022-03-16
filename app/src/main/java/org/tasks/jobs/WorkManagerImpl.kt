package org.tasks.jobs

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkContinuation
import androidx.work.WorkInfo
import androidx.work.WorkRequest
import androidx.work.Worker
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskListDao
import org.tasks.data.OpenTaskDao
import org.tasks.data.Place
import org.tasks.date.DateTimeUtils.midnight
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.jobs.MigrateLocalWork.Companion.EXTRA_ACCOUNT
import org.tasks.jobs.SyncWork.Companion.EXTRA_BACKGROUND
import org.tasks.jobs.SyncWork.Companion.EXTRA_IMMEDIATE
import org.tasks.jobs.WorkManager.Companion.MAX_CLEANUP_LENGTH
import org.tasks.jobs.WorkManager.Companion.REMOTE_CONFIG_INTERVAL_HOURS
import org.tasks.jobs.WorkManager.Companion.TAG_BACKGROUND_SYNC_CALDAV
import org.tasks.jobs.WorkManager.Companion.TAG_BACKGROUND_SYNC_ETEBASE
import org.tasks.jobs.WorkManager.Companion.TAG_BACKGROUND_SYNC_GOOGLE_TASKS
import org.tasks.jobs.WorkManager.Companion.TAG_BACKGROUND_SYNC_OPENTASKS
import org.tasks.jobs.WorkManager.Companion.TAG_BACKUP
import org.tasks.jobs.WorkManager.Companion.TAG_MIDNIGHT_REFRESH
import org.tasks.jobs.WorkManager.Companion.TAG_MIGRATE_LOCAL
import org.tasks.jobs.WorkManager.Companion.TAG_REFRESH
import org.tasks.jobs.WorkManager.Companion.TAG_REMOTE_CONFIG
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_CALDAV
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_ETEBASE
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_GOOGLE_TASKS
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_OPENTASK
import org.tasks.jobs.WorkManager.Companion.TAG_UPDATE_PURCHASES
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
        private val caldavDao: CaldavDao,
        private val openTaskDao: OpenTaskDao
): WorkManager {
    private val throttle = Throttle(200, 60000, "WORK")
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val workManager = androidx.work.WorkManager.getInstance(context)

    override fun scheduleRepeat(task: Task) {
        enqueue(
                OneTimeWorkRequest.Builder(AfterSaveWork::class.java)
                        .setInputData(Data.Builder()
                                .putLong(AfterSaveWork.EXTRA_ID, task.id)
                                .putBoolean(
                                    AfterSaveWork.EXTRA_SUPPRESS_COMPLETION_SNACKBAR,
                                    task.isSuppressRefresh()
                                )
                                .build()))
    }

    override fun updateCalendar(task: Task) {
        enqueue(
                OneTimeWorkRequest.Builder(UpdateCalendarWork::class.java)
                        .setInputData(Data.Builder()
                                .putLong(UpdateCalendarWork.EXTRA_ID, task.id)
                                .build()))
    }

    @SuppressLint("EnqueueWork")
    override fun migrateLocalTasks(caldavAccount: CaldavAccount) {
        val builder = OneTimeWorkRequest.Builder(MigrateLocalWork::class.java)
                .setInputData(Data.Builder().putString(EXTRA_ACCOUNT, caldavAccount.uuid).build())
                .setConstraints(networkConstraints)
        enqueue(workManager.beginUniqueWork(TAG_MIGRATE_LOCAL, APPEND_OR_REPLACE, builder.build()))
    }

    override fun cleanup(ids: Iterable<Long>) {
        ids.chunked(MAX_CLEANUP_LENGTH) {
            enqueue(
                    OneTimeWorkRequest.Builder(CleanupWork::class.java)
                            .setInputData(
                                    Data.Builder()
                                            .putLongArray(CleanupWork.EXTRA_TASK_IDS, it.toLongArray())
                                            .build()))
        }
    }

    override suspend fun googleTaskSync(immediate: Boolean) =
            sync(immediate, TAG_SYNC_GOOGLE_TASKS, SyncGoogleTasksWork::class.java)

    override suspend fun caldavSync(immediate: Boolean) =
            sync(immediate, TAG_SYNC_CALDAV, SyncCaldavWork::class.java)

    override suspend fun eteBaseSync(immediate: Boolean) =
            sync(immediate, TAG_SYNC_ETEBASE, SyncEtebaseWork::class.java)

    override suspend fun openTaskSync(immediate: Boolean) =
            sync(immediate, TAG_SYNC_OPENTASK, SyncOpenTasksWork::class.java, false)

    @SuppressLint("EnqueueWork")
    private suspend fun sync(immediate: Boolean, tag: String, c: Class<out SyncWork>, requireNetwork: Boolean = true) {
        Timber.d("sync(immediate = $immediate, $tag, $c, requireNetwork = $requireNetwork)")
        val builder = OneTimeWorkRequest.Builder(c)
                .setInputData(Data.Builder().putBoolean(EXTRA_IMMEDIATE, immediate).build())
        if (requireNetwork) {
            builder.setConstraints(networkConstraints)
        }
        if (!immediate) {
            builder.setInitialDelay(1, TimeUnit.MINUTES)
        }
        val append = withContext(Dispatchers.IO) {
            workManager.getWorkInfosByTag(tag).get().any {
                it.state == WorkInfo.State.RUNNING
            }
        }
        enqueue(workManager.beginUniqueWork(
                tag,
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
                        .setInputData(Data.Builder().putLong(ReverseGeocodeWork.PLACE_ID, place.id).build())
                        .setConstraints(networkConstraints))
    }

    override fun updateBackgroundSync() {
        throttle.run {
            scheduleBackgroundSync(
                    TAG_BACKGROUND_SYNC_GOOGLE_TASKS,
                    SyncGoogleTasksWork::class.java,
                    googleTaskListDao.accountCount() > 0)
        }
        throttle.run {
            scheduleBackgroundSync(
                    TAG_BACKGROUND_SYNC_CALDAV,
                    SyncCaldavWork::class.java,
                    caldavDao.getAccounts(TYPE_CALDAV, TYPE_TASKS).isNotEmpty())
        }
        throttle.run {
            scheduleBackgroundSync(
                    TAG_BACKGROUND_SYNC_ETEBASE,
                    SyncEtebaseWork::class.java,
                    caldavDao.getAccounts(TYPE_ETEBASE).isNotEmpty())
        }
        throttle.run {
            scheduleBackgroundSync(
                    TAG_BACKGROUND_SYNC_OPENTASKS,
                    SyncOpenTasksWork::class.java,
                    openTaskDao.shouldSync()
            )
        }
    }

    private fun scheduleBackgroundSync(tag: String, c: Class<out SyncWork>, enabled: Boolean) {
        Timber.d("scheduleBackgroundSync($tag, $c, enabled = $enabled)")
        if (enabled) {
            val builder = PeriodicWorkRequest.Builder(c, 1, TimeUnit.HOURS)
                    .setInputData(Data.Builder().putBoolean(EXTRA_BACKGROUND, true).build())
                    .setConstraints(networkConstraints)
            workManager.enqueueUniquePeriodicWork(
                    tag, ExistingPeriodicWorkPolicy.KEEP, builder.build())
        } else {
            workManager.cancelUniqueWork(tag)
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
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
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
                .setInputData(DriveUploader.getInputData(uri, purge))
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
}