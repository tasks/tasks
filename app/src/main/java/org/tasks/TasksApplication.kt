package org.tasks

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.JobIntentService
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.work.Configuration
import com.mikepenz.iconics.Iconics
import com.todoroo.andlib.utility.AndroidUtilities.atLeastR
import com.todoroo.astrid.service.Upgrader
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.tasks.billing.Inventory
import org.tasks.caldav.CaldavSynchronizer
import org.tasks.icons.OutlinedGoogleMaterial
import org.tasks.icons.OutlinedGoogleMaterial2
import org.tasks.injection.InjectingJobIntentService
import org.tasks.jobs.WorkManager
import org.tasks.location.GeofenceApi
import org.tasks.opentasks.OpenTaskContentObserver
import org.tasks.preferences.Preferences
import org.tasks.receivers.RefreshReceiver
import org.tasks.scheduling.NotificationSchedulerIntentService
import org.tasks.sync.SyncAdapters
import org.tasks.themes.ThemeBase
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.widget.AppWidgetManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class TasksApplication : Application(), Configuration.Provider {

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var buildSetup: BuildSetup
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var upgrader: Lazy<Upgrader>
    @Inject lateinit var workManager: Lazy<WorkManager>
    @Inject lateinit var geofenceApi: Lazy<GeofenceApi>
    @Inject lateinit var appWidgetManager: Lazy<AppWidgetManager>
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var contentObserver: Lazy<OpenTaskContentObserver>
    @Inject lateinit var syncAdapters: Lazy<SyncAdapters>

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        buildSetup.setup()
        upgrade()
        preferences.isSyncOngoing = false
        ThemeBase.getThemeBase(preferences, inventory, null).setDefaultNightMode()
        localBroadcastManager.registerRefreshReceiver(RefreshBroadcastReceiver())
        backgroundWork()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    Timber.d("Application.onResume")
                    localBroadcastManager.broadcastRefresh()
                    if (currentTimeMillis() - preferences.lastSync > TimeUnit.MINUTES.toMillis(5)) {
                        syncAdapters.get().sync(true)
                    }
                }

                override fun onPause(owner: LifecycleOwner) {
                    Timber.d("Application.onPause")
                    owner.lifecycle.coroutineScope.launch {
                        workManager.get().startEnqueuedSync()
                    }
                }
            }
        )
    }

    private fun upgrade() {
        val lastVersion = preferences.lastSetVersion
        val currentVersion = BuildConfig.VERSION_CODE
        Timber.i("Astrid Startup. %s => %s", lastVersion, currentVersion)
        if (atLeastR()) {
            scope.launch {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val exitReasons = activityManager.getHistoricalProcessExitReasons(null, 0, 1)
                logExitReasons(exitReasons)
            }
        }

        // invoke upgrade service
        if (lastVersion != currentVersion) {
            upgrader.get().upgrade(lastVersion, currentVersion)
            scope.launch {
                preferences.setDefaults()
            }
        }
    }

    private fun backgroundWork() = scope.launch {
        Iconics.registerFont(OutlinedGoogleMaterial)
        Iconics.registerFont(OutlinedGoogleMaterial2)
        inventory.updateTasksAccount()
        NotificationSchedulerIntentService.enqueueWork(context)
        workManager.get().apply {
            updateBackgroundSync()
            scheduleBackup()
            scheduleConfigRefresh()
            updatePurchases()
            scheduleRefresh()
        }
        OpenTaskContentObserver.registerObserver(context, contentObserver.get())
        geofenceApi.get().registerAll()
        appWidgetManager.get().reconfigureWidgets()
        CaldavSynchronizer.registerFactories()
    }

    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .build()

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)

        localBroadcastManager.reconfigureWidgets()
    }

    private class RefreshBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            JobIntentService.enqueueWork(
                    context,
                    RefreshReceiver::class.java,
                    InjectingJobIntentService.JOB_ID_REFRESH_RECEIVER,
                    intent)
        }
    }

    companion object {
        @Suppress("KotlinConstantConditions")
        const val IS_GOOGLE_PLAY = BuildConfig.FLAVOR == "googleplay"
        @Suppress("KotlinConstantConditions")
        const val IS_GENERIC = BuildConfig.FLAVOR == "generic"
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun logExitReasons(exitReasons: List<ApplicationExitInfo>) {
    exitReasons.forEach { info ->
        Timber.i("""
            Exit reason: ${info.reason.toReasonString()}
            Description: ${info.description}
            Timestamp: ${info.timestamp}
            Process: ${info.processName}
            PSS: ${info.pss}
            RSS: ${info.rss}
            Importance: ${info.importance}
            Status: ${info.status}
            Trace: ${info.traceInputStream?.bufferedReader()?.readText()}
        """.trimIndent())
    }
}

private fun Int.toReasonString() = when (this) {
    ApplicationExitInfo.REASON_ANR -> "ANR"
    ApplicationExitInfo.REASON_CRASH -> "CRASH"
    ApplicationExitInfo.REASON_CRASH_NATIVE -> "NATIVE_CRASH"
    ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
    ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
    ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
    ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
    ApplicationExitInfo.REASON_OTHER -> "OTHER"
    ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
    ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
    ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
    ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
    else -> "UNKNOWN($this)"
}
