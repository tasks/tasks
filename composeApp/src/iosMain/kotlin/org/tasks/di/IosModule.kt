package org.tasks.di

import androidx.room3.Room
import com.todoroo.astrid.service.CommonUpgrades
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
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
import org.tasks.auth.AppleAuthorizer
import org.tasks.auth.AuthSessionRedirect
import org.tasks.auth.OAuthProvider
import org.tasks.auth.OAuthRedirect
import org.tasks.auth.OAuthSignInHandler
import org.tasks.auth.OAuthTokenStore
import org.tasks.auth.SignInHandler
import org.tasks.auth.TasksOAuthClient
import org.tasks.auth.TasksOAuthSignIn
import org.tasks.auth.authenticate
import org.tasks.auth.oauthDefaults
import org.tasks.billing.BillingProvider
import org.tasks.billing.EntitlementSubscriptionProvider
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.GitHubSponsorOAuthClient
import org.tasks.billing.GitHubSponsorVerifier
import org.tasks.billing.PurchaseEntitlement
import org.tasks.billing.SubscriptionProvider
import org.tasks.billing.debugProFlow
import org.tasks.caldav.CaldavClientFactory
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.CaldavSynchronizer
import org.tasks.caldav.FileStorage
import org.tasks.caldav.VtodoCache
import org.tasks.compose.accounts.Platform
import org.tasks.data.TaskCreator
import org.tasks.data.db.CommonMigrations
import org.tasks.data.db.Database
import org.tasks.data.db.setBundledDriver
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Task
import org.tasks.fcm.ApnsTokenProvider
import org.tasks.fcm.FcmTokenProvider
import org.tasks.googleapis.GoogleTasksAccountSynchronizer
import org.tasks.googleapis.KtorGoogleTasksApiFactory
import org.tasks.googleapis.OAuthTokenAccountAccess
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
import org.tasks.security.SecureEnclaveEncryption
import org.tasks.service.TaskCleanup
import org.tasks.service.TaskMigrator
import org.tasks.service.Upgrader
import org.tasks.sse.SseClient
import org.tasks.sync.SyncRunner
import org.tasks.sync.SyncSource
import org.tasks.viewmodel.GoogleTaskListSettingsViewModel
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
            supportsGoogleTasks = true,
            supportsNotifications = false,
            supportsLogExport = true,
            billingProvider = BillingProvider.STRIPE,
            appStore = AppStore.APP_STORE,
            supportsPlayLinkRestore = true,
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
            .setBundledDriver()
            .addMigrations(*CommonMigrations.all)
            .addCallback(Database.CALLBACK)
            .build()
    }
    single { TasksPreferences(createDataStore { "$documentsPath/$dataStoreFileName" }) }
    factory { Upgrader(get(), get(), CommonUpgrades.all(get(), get(), get())) }
    factory { FileStorage(documentsPath) }
    factoryOf(::VtodoCache)
    single<Encryption> { SecureEnclaveEncryption() }
    single<KtorClientFactory> { DarwinKtorClientFactory() }
    factory<CaldavClientProvider> { CaldavClientProvider(get(), get(), get(), get(), get()) }
    single { ApnsTokenProvider(tasksPreferences = get(), installId = get(), scope = get(), pushTokenManager = { get() }, sseClient = { get() }) }
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
    single {
        val factory = get<KtorClientFactory>()
        TasksOAuthClient { factory.newClient { oauthDefaults() } }
    }
    single {
        TasksOAuthSignIn(
            caldavDao = get(),
            encryption = get(),
            serverEnvironment = get(),
            clientFactory = get(),
            purchaseClaim = get(),
            pushTokenManager = get(),
            analytics = get<Reporting>(),
            oauthClient = get(),
            redirect = get(),
            nativeAuthorizers = mapOf(OAuthProvider.APPLE to AppleAuthorizer(get(), get())),
        )
    }
    single {
        KtorGoogleTasksApiFactory(
            httpClientFactory = get(),
            tokenStore = OAuthTokenStore(
                encryption = get(),
                caldavDao = get(),
                oauthClient = get(),
                refreshWhenExpiryUnknown = false,
            ),
        )
    }
    factory {
        GoogleTasksAccountSynchronizer(
            caldavDao = get(),
            taskDao = get(),
            dirtyDao = get(),
            taskSaver = get(),
            reporting = get(),
            googleTaskDao = get(),
            defaultListProvider = get(),
            refreshBroadcaster = get(),
            taskDeleter = get(),
            alarmDao = get(),
            appPreferences = get(),
            repeatTaskHelper = get(),
            taskCompleter = get(),
            createTask = { TaskCreator().createBlankTask() },
            credentials = OAuthTokenAccountAccess,
            api = { account -> get<KtorGoogleTasksApiFactory>().forAccount(account) },
        )
    }
    viewModel { params ->
        GoogleTaskListSettingsViewModel(
            caldavDao = get(),
            taskDeleter = get(),
            reporting = get(),
            purchaseState = get(),
            invokerFactory = { account -> get<KtorGoogleTasksApiFactory>().forAccount(account) },
            isDark = params.get(),
            account = params.get(),
            calendar = params.get(),
        )
    }
    factory<SignInHandler>(PlatformSignInHandler) {
        OAuthSignInHandler(
            signIn = get<TasksOAuthSignIn>(),
            oauthPlatforms = setOf(Platform.TASKS_ORG, Platform.GOOGLE_TASKS),
        )
    }
    single {
        PurchaseEntitlement(
            dataDir = documentsPath,
            encryption = get(),
            httpClientFactory = get(),
            serverEnvironment = get(),
            scope = get(),
            json = get(),
            syncAdapters = { get() },
        ).also { it.initialize() }
    }
    single<OAuthRedirect> { AuthSessionRedirect(authenticate = ::authenticate) }
    single<GitHubSponsorClient> {
        GitHubSponsorOAuthClient(
            verify = get<GitHubSponsorVerifier>()::verify,
            redirect = get(),
        )
    }
    single<SubscriptionProvider> {
        EntitlementSubscriptionProvider(
            entitlement = get(),
            debugPro = get<TasksPreferences>().debugProFlow,
        )
    }
    single {
        SyncRunner(get(), get(), get(), { get() }) { pass ->
            val synchronizer = get<CaldavSynchronizer>()
            pass.accounts(CaldavAccount.TYPE_CALDAV, CaldavAccount.TYPE_TASKS).forEach { account ->
                synchronizer.sync(account, hasPro = pass.hasPro)
            }
            pass.accounts(CaldavAccount.TYPE_GOOGLE_TASKS).forEach { account ->
                get<GoogleTasksAccountSynchronizer>().sync(account, hasPro = pass.googleTasksPro)
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
            googleTaskDao = get(),
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
