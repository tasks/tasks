package org.tasks.jobs

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.*
import androidx.work.WorkManager
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskListDao
import org.tasks.data.Place
import org.tasks.date.DateTimeUtils.midnight
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.injection.ApplicationScope
import org.tasks.injection.ApplicationContext
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max

@ApplicationScope
class WorkManager @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val googleTaskListDao: GoogleTaskListDao,
        private val caldavDao: CaldavDao) {

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val workManager: WorkManager = WorkManager.getInstance(context)

    fun afterSave(current: Task, original: Task?) =
            workManager.enqueue(
                    OneTimeWorkRequest.Builder(AfterSaveWork::class.java)
                            .setInputData(AfterSaveWork.getInputData(current, original))
                            .build())

    fun cleanup(ids: Iterable<Long>) = ids.chunked(MAX_CLEANUP_LENGTH) {
        workManager.enqueue(
                OneTimeWorkRequest.Builder(CleanupWork::class.java)
                        .setInputData(
                                Data.Builder()
                                        .putLongArray(CleanupWork.EXTRA_TASK_IDS, it.toLongArray())
                                        .build())
                        .build())
    }

    fun sync(immediate: Boolean) {
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
        val request = builder.build()
        workManager.beginUniqueWork(TAG_SYNC, ExistingWorkPolicy.REPLACE, request).enqueue()
    }

    fun reverseGeocode(place: Place) {
        if (BuildConfig.DEBUG && place.id == 0L) {
            throw RuntimeException("Missing id")
        }
        workManager.enqueue(
                OneTimeWorkRequest.Builder(ReverseGeocodeWork::class.java)
                        .setInputData(Data.Builder().putLong(ReverseGeocodeWork.PLACE_ID, place.id).build())
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                        .setConstraints(
                                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build())
    }

    fun updateBackgroundSync() = updateBackgroundSync(null, null, null)

    @SuppressLint("CheckResult")
    fun updateBackgroundSync(
            forceAccountPresent: Boolean?,
            forceBackgroundEnabled: Boolean?,
            forceOnlyOnUnmetered: Boolean?) {
        val backgroundEnabled = forceBackgroundEnabled
                ?: preferences.getBoolean(R.string.p_background_sync, true)
        val onlyOnWifi = forceOnlyOnUnmetered
                ?: preferences.getBoolean(R.string.p_background_sync_unmetered_only, false)
        (if (forceAccountPresent == null) Single.zip(
                googleTaskListDao.accountCount(),
                Single.fromCallable { caldavDao.accountCount() },
                BiFunction { googleCount: Int, caldavCount: Int -> googleCount > 0 || caldavCount > 0 }) else Single.just(forceAccountPresent))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { accountsPresent: Boolean -> scheduleBackgroundSync(backgroundEnabled && accountsPresent, onlyOnWifi) }
    }

    private fun scheduleBackgroundSync(enabled: Boolean, onlyOnUnmetered: Boolean) {
        Timber.d("background sync enabled: %s, onlyOnUnmetered: %s", enabled, onlyOnUnmetered)
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

    fun scheduleRefresh(time: Long) = enqueueUnique(TAG_REFRESH, RefreshWork::class.java, time)

    fun scheduleMidnightRefresh() =
            enqueueUnique(TAG_MIDNIGHT_REFRESH, MidnightRefreshWork::class.java, midnight())

    fun scheduleNotification(scheduledTime: Long) {
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

    fun scheduleBackup() {
        enqueueUnique(
                TAG_BACKUP,
                BackupWork::class.java,
                newDateTime(preferences.getLong(R.string.p_last_backup, 0L))
                        .plusDays(1)
                        .millis
                        .coerceAtMost(midnight()))
    }

    fun scheduleConfigRefresh() =
            workManager.enqueueUniquePeriodicWork(
                    TAG_REMOTE_CONFIG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequest.Builder(
                            RemoteConfigWork::class.java, REMOTE_CONFIG_INTERVAL_HOURS, TimeUnit.HOURS)
                            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                            .setConstraints(
                                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                            .build())

    fun scheduleDriveUpload(uri: Uri?, purge: Boolean) {
        if (!preferences.getBoolean(R.string.p_google_drive_backup, false)) {
            return
        }
        val builder = OneTimeWorkRequest.Builder(DriveUploader::class.java)
                .setInputData(DriveUploader.getInputData(uri, purge))
                .setConstraints(networkConstraints)
        if (purge) {
            builder.setInitialDelay(Random().nextInt(3600).toLong(), TimeUnit.SECONDS)
        }
        workManager.enqueue(builder.build())
    }

    private val networkConstraints: Constraints
        get() = getNetworkConstraints(
                preferences.getBoolean(R.string.p_background_sync_unmetered_only, false))

    private fun getNetworkConstraints(unmeteredOnly: Boolean) =
            Constraints.Builder()
                    .setRequiredNetworkType(if (unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .build()

    private fun enqueueUnique(key: String, c: Class<out Worker?>, time: Long) {
        val delay = time - DateUtilities.now()
        val builder = OneTimeWorkRequest.Builder(c)
        if (delay > 0) {
            builder.setInitialDelay(delay, TimeUnit.MILLISECONDS)
        }
        Timber.d("$key: ${DateTimeUtils.printTimestamp(time)} (${DateTimeUtils.printDuration(delay)})")
        workManager.beginUniqueWork(key, ExistingWorkPolicy.REPLACE, builder.build()).enqueue()
    }

    fun cancelNotifications() {
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

    companion object {
        val REMOTE_CONFIG_INTERVAL_HOURS = if (BuildConfig.DEBUG) 1 else 12.toLong()
        private const val MAX_CLEANUP_LENGTH = 500
        private const val TAG_BACKUP = "tag_backup"
        private const val TAG_REFRESH = "tag_refresh"
        private const val TAG_MIDNIGHT_REFRESH = "tag_midnight_refresh"
        private const val TAG_SYNC = "tag_sync"
        private const val TAG_BACKGROUND_SYNC = "tag_background_sync"
        private const val TAG_REMOTE_CONFIG = "tag_remote_config"
    }
}