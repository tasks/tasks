package org.tasks.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.todoroo.astrid.service.CommonUpgrades
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.tasks.AppStore
import org.tasks.PlatformConfiguration
import org.tasks.TasksBuildConfig
import org.tasks.analytics.Analytics
import org.tasks.analytics.IosReporting
import org.tasks.analytics.Reporting
import org.tasks.analytics.crashlyticsInstalled
import org.tasks.analytics.installedAnalytics
import org.tasks.api.ApiQueryEngine
import org.tasks.api.ApiTaskFactory
import org.tasks.api.ApiWriter
import org.tasks.api.DatabaseApiTaskFactory
import org.tasks.api.ListManager
import org.tasks.api.LocalListManager
import org.tasks.auth.IosOAuthFlow
import org.tasks.auth.IosSignInHandler
import org.tasks.auth.OAuthFlow
import org.tasks.auth.SignInHandler
import org.tasks.auth.TasksOAuthClient
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.CaldavClientFactory
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.CaldavSynchronizer
import org.tasks.caldav.FileStorage
import org.tasks.caldav.VtodoCache
import org.tasks.data.TaskCreator
import org.tasks.data.db.CommonMigrations
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Task
import org.tasks.fcm.ApnsTokenProvider
import org.tasks.fcm.FcmTokenProvider
import org.tasks.http.DarwinKtorClientFactory
import org.tasks.http.KtorClientFactory
import org.tasks.jobs.BackgroundWork
import org.tasks.kmp.IosBuildConfig
import org.tasks.kmp.createDataStore
import org.tasks.kmp.dataStoreFileName
import org.tasks.logging.IosLogExporter
import org.tasks.logging.LogExporter
import org.tasks.logging.fileLogWriter
import org.tasks.notifications.CancelReason
import org.tasks.notifications.Notifier
import org.tasks.preferences.TasksPreferences
import org.tasks.security.Encryption
import org.tasks.security.PlainTextEncryption
import org.tasks.service.TaskCleanup
import org.tasks.service.TaskMigrator
import org.tasks.service.Upgrader
import org.tasks.sse.SseClient
import org.tasks.sync.SyncRunner
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
            supportsCaldav = true,
            supportsNotifications = false,
            supportsLogExport = true,
            appStore = AppStore.APP_STORE,
        )
    }
    single<LogExporter> { IosLogExporter(fileLogWriter) }
    single<Reporting>(createdAtStart = true) {
        IosReporting(
            tasksPreferences = get(),
            crashlytics = crashlyticsInstalled,
            analytics = installedAnalytics,
            posthogKey = IosBuildConfig.POSTHOG_KEY,
        )
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
    single<Encryption> { PlainTextEncryption() }
    single<KtorClientFactory> { DarwinKtorClientFactory() }
    factory<CaldavClientProvider> { CaldavClientProvider(get(), get(), get(), get(), get()) }
    single { ApnsTokenProvider(tasksPreferences = get(), scope = get(), pushTokenManager = { get() }, sseClient = { get() }) }
    single {
        SseClient(
            scope = get(),
            backgroundWork = get(),
            caldavDao = get(),
            encryption = get(),
            environment = get(),
            httpClientFactory = get(),
            token = { get<FcmTokenProvider>().getToken() },
        )
    }
    single<FcmTokenProvider> { get<ApnsTokenProvider>() }
    factory<CaldavClientFactory> { get<CaldavClientProvider>() }
    single { TasksOAuthClient() }
    factory<OAuthFlow> { IosOAuthFlow(get(), get()) }
    factory<SignInHandler> { IosSignInHandler(get(), get(), get(), get(), get(), get()) }
    single<SubscriptionProvider> {
        val tasksPreferences = get<TasksPreferences>()
        val subscriptionFlow: Flow<SubscriptionProvider.SubscriptionInfo?> =
            if (TasksBuildConfig.DEBUG) {
                tasksPreferences.flow(TasksPreferences.debugPro, false).map { debug ->
                    if (debug) {
                        SubscriptionProvider.SubscriptionInfo(
                            sku = "debug_pro",
                            isMonthly = false,
                            isTasksSubscription = false,
                            isGitHubSponsor = false,
                        )
                    } else {
                        null
                    }
                }
            } else {
                flowOf(null)
            }
        object : SubscriptionProvider {
            override val subscription: Flow<SubscriptionProvider.SubscriptionInfo?> = subscriptionFlow
            override suspend fun getFormattedPrice(sku: String): String? = null
        }
    }
    single {
        SyncRunner(get(), get(), { get() }) { pass ->
            val synchronizer = get<CaldavSynchronizer>()
            pass.accounts(CaldavAccount.TYPE_CALDAV, CaldavAccount.TYPE_TASKS).forEach { account ->
                synchronizer.sync(account, hasPro = pass.hasPro)
            }
        }
    }
    single<BackgroundWork> {
        val scope = get<CoroutineScope>()
        val runner = get<SyncRunner>()
        object : BackgroundWork {
            override fun updateCalendar(task: Task) {}
            override suspend fun scheduleRefresh(timestamp: Long) {}
            override suspend fun sync(source: SyncSource) = runner.sync(source)
            override suspend fun scheduleBlogFeedCheck() {}
            override fun migrateLocalTasks(localAccount: CaldavAccount, tasksAccount: CaldavAccount) {
                scope.launch { get<TaskMigrator>().migrateLocalTasks(localAccount, tasksAccount) }
            }
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
    single { get<Database>().apiDao() }
    single { ApiQueryEngine(get()) }
    single<ApiTaskFactory> {
        DatabaseApiTaskFactory(
            taskDao = get(),
            caldavDao = get(),
            taskCreator = TaskCreator(),
            taskSaver = get(),
            defaultListProvider = get(),
            appPreferences = get(),
        )
    }
    single<ListManager> { LocalListManager(caldavDao = get(), taskDeleter = get()) }
    single {
        ApiWriter(
            apiDao = get(),
            taskDao = get(),
            caldavDao = get(),
            tagDao = get(),
            tagDataDao = get(),
            alarmDao = get(),
            locationDao = get(),
            taskFactory = get(),
            taskSaver = get(),
            taskCompleter = get(),
            taskMover = get(),
            taskDeleter = get(),
            alarmService = get(),
            locationService = get(),
            listManager = get(),
            tagMetadataEditor = get(),
        )
    }
}
