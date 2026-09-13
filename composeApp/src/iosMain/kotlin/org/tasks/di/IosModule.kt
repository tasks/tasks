package org.tasks.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.todoroo.astrid.service.CommonUpgrades
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.tasks.PlatformConfiguration
import org.tasks.TasksBuildConfig
import org.tasks.analytics.Analytics
import org.tasks.analytics.Reporting
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.FileStorage
import org.tasks.caldav.metadata.TagMetadataEditor
import org.tasks.caldav.VtodoCache
import org.tasks.data.db.CommonMigrations
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Task
import org.tasks.jobs.BackgroundWork
import org.tasks.kmp.createDataStore
import org.tasks.kmp.dataStoreFileName
import org.tasks.notifications.CancelReason
import org.tasks.notifications.Notifier
import org.tasks.preferences.TasksPreferences
import org.tasks.service.TaskCleanup
import org.tasks.service.Upgrader
import org.tasks.sync.SyncSource
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

internal val documentsPath: String by lazy {
    val url = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL
    url.path ?: error("no Documents directory")
}

actual fun platformModule(): Module = module {
    single {
        PlatformConfiguration(
            versionCode = TasksBuildConfig.VERSION_CODE,
            supportsTasksOrg = false,
            supportsNotifications = false,
        )
    }
    single<Reporting> {
        object : Reporting {
            override val tasksPreferences: TasksPreferences = get()
            override fun logEvent(event: String, vararg params: Pair<String, Any>) {}
            override fun addTask(source: String) {}
            override fun completeTask(source: String) {}
            override fun identify(distinctId: String) {}
            override fun reportException(t: Throwable, fatal: Boolean) {
                co.touchlab.kermit.Logger.e(t) { "reported" }
            }
        }
    }
    single<Analytics> { get<Reporting>() }
    single<Database> {
        Room.databaseBuilder<Database>(name = "$documentsPath/${Database.NAME}")
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*CommonMigrations.all)
            .addCallback(Database.CALLBACK)
            .build()
    }
    single { TasksPreferences(createDataStore { "$documentsPath/$dataStoreFileName" }) }
    factory { Upgrader(get(), CommonUpgrades.all(get())) }
    factory { FileStorage(documentsPath) }
    factoryOf(::VtodoCache)
    single { TagMetadataEditor(get(), get(), get()) }
    single<SubscriptionProvider> {
        object : SubscriptionProvider {
            override val subscription: Flow<SubscriptionProvider.SubscriptionInfo?> = flowOf(null)
            override suspend fun getFormattedPrice(sku: String): String? = null
        }
    }
    single<BackgroundWork> {
        object : BackgroundWork {
            override fun updateCalendar(task: Task) {}
            override suspend fun scheduleRefresh(timestamp: Long) {}
            override suspend fun sync(source: SyncSource) {}
            override suspend fun scheduleBlogFeedCheck() {}
            override fun migrateLocalTasks(localAccount: CaldavAccount, tasksAccount: CaldavAccount) {}
        }
    }
    factory<Notifier> {
        object : Notifier {
            override suspend fun cancel(id: Long, reason: CancelReason) {}
            override suspend fun cancel(ids: List<Long>, reason: CancelReason) {}
            override suspend fun cancelAll(reason: CancelReason) {}
            override fun triggerNotifications() {}
            override suspend fun updateTimerNotification() {}
        }
    }
    factory<TaskCleanup> { object : TaskCleanup {} }
}
