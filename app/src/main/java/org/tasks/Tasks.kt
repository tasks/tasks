package org.tasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.work.Configuration
import com.todoroo.astrid.service.Upgrader
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.files.FileHelper
import org.tasks.injection.ApplicationComponent
import org.tasks.injection.ApplicationContext
import org.tasks.injection.InjectingApplication
import org.tasks.injection.InjectingJobIntentService
import org.tasks.jobs.WorkManager
import org.tasks.location.GeofenceApi
import org.tasks.preferences.Preferences
import org.tasks.receivers.RefreshReceiver
import org.tasks.scheduling.CalendarNotificationIntentService
import org.tasks.scheduling.NotificationSchedulerIntentService
import org.tasks.scheduling.RefreshScheduler
import org.tasks.themes.ThemeBase
import org.tasks.widget.AppWidgetManager
import timber.log.Timber
import javax.inject.Inject

class Tasks : InjectingApplication(), Configuration.Provider {
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var buildSetup: BuildSetup
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var upgrader: Lazy<Upgrader>
    @Inject lateinit var workManager: Lazy<WorkManager>
    @Inject lateinit var refreshScheduler: Lazy<RefreshScheduler>
    @Inject lateinit var geofenceApi: Lazy<GeofenceApi>
    @Inject lateinit var billingClient: Lazy<BillingClient>
    @Inject lateinit var appWidgetManager: Lazy<AppWidgetManager>
    
    override fun onCreate() {
        super.onCreate()
        buildSetup.setup()
        upgrade()
        preferences.isSyncOngoing = false
        ThemeBase.getThemeBase(preferences, inventory, null).setDefaultNightMode()
        localBroadcastManager.registerRefreshReceiver(RefreshBroadcastReceiver())
        Completable.fromAction { doInBackground() }.subscribeOn(Schedulers.io()).subscribe()
    }

    private fun upgrade() {
        val lastVersion = preferences.lastSetVersion
        val currentVersion = BuildConfig.VERSION_CODE
        Timber.i("Astrid Startup. %s => %s", lastVersion, currentVersion)

        // invoke upgrade service
        if (lastVersion != currentVersion) {
            upgrader.get().upgrade(lastVersion, currentVersion)
            preferences.setDefaults()
        }
    }

    private fun doInBackground() {
        NotificationSchedulerIntentService.enqueueWork(context, false)
        CalendarNotificationIntentService.enqueueWork(context)
        refreshScheduler.get().scheduleAll()
        workManager.get().updateBackgroundSync()
        workManager.get().scheduleMidnightRefresh()
        workManager.get().scheduleBackup()
        workManager.get().scheduleConfigRefresh()
        geofenceApi.get().registerAll()
        FileHelper.delete(context, preferences.cacheDirectory)
        billingClient.get().queryPurchases()
        appWidgetManager.get().reconfigureWidgets()
    }

    override fun inject(component: ApplicationComponent) = component.inject(this)

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
                .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
                .build()
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
}