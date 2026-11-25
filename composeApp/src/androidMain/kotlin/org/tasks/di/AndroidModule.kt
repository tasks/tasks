package org.tasks.di

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.room3.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.tasks.analytics.Reporting
import org.tasks.auth.AndroidSignInHandler
import org.tasks.auth.OAuthRedirect
import org.tasks.auth.SchemeRedirect
import org.tasks.auth.SignInHandler
import org.tasks.billing.CompositeSubscriptionProvider
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.GitHubSponsorVerifier
import org.tasks.billing.GitHubSponsorOAuthClient
import org.tasks.billing.EntitlementSubscriptionProvider
import org.tasks.billing.debugProFlow
import org.tasks.billing.PurchaseEntitlement
import org.tasks.billing.StoreSubscriptionProvider
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.FileStorage
import org.tasks.caldav.VtodoCache
import org.tasks.data.OpenTaskDao
import org.tasks.etebase.EtebaseClientProvider
import org.tasks.googleapis.GoogleTasksCredentialsProvider
import org.tasks.gtasks.AccountManagerCredentials
import org.tasks.gtasks.GoogleAccountManager
import org.tasks.gtasks.GoogleAccountPicker
import org.tasks.opentasks.OpenTasksSyncer
import org.tasks.opentasks.OpenTasksSynchronizer
import org.tasks.http.AndroidOkHttpClientFactory
import org.tasks.http.OkHttpClientFactory
import org.tasks.auth.tasksOAuthSignIn
import org.tasks.data.db.CommonMigrations
import org.tasks.data.db.Database
import org.tasks.data.db.setBundledDriver
import org.tasks.kmp.createDataStore
import org.tasks.notifications.CancelReason
import org.tasks.notifications.Notifier
import org.tasks.preferences.TasksPreferences
import org.tasks.security.AndroidKeyStoreEncryption
import org.tasks.security.KeyStoreEncryption
import org.tasks.service.TaskCleanup

actual fun platformModule(): Module = module {
    includes(flavorModule)
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
    factory<Reporting> {
        val tasksPreferences = get<TasksPreferences>()
        object : Reporting {
            override val tasksPreferences = tasksPreferences
            override val analyticsEnabled = false
            override fun logEvent(event: String, vararg params: Pair<String, Any>) {}
            override fun addTask(source: String) {}
            override fun completeTask(source: String) {}
            override fun setSku(sku: String?) {}
            override fun reportException(t: Throwable, fatal: Boolean) {}
        }
    }
    single<KeyStoreEncryption> { AndroidKeyStoreEncryption() }
    single<GitHubSponsorClient> {
        GitHubSponsorOAuthClient(
            verify = get<GitHubSponsorVerifier>()::verify,
            redirect = get<SchemeRedirect>(),
        )
    }
    single { SchemeRedirect(androidContext()) }
    single<OAuthRedirect> { get<SchemeRedirect>() }
    single {
        get<OkHttpClientFactory>().tasksOAuthSignIn(
            caldavDao = get(),
            encryption = get<KeyStoreEncryption>(),
            serverEnvironment = get(),
            clientFactory = get(),
            purchaseClaim = get(),
            pushTokenManager = get(),
            analytics = get<Reporting>(),
            redirect = get(),
        )
    }
    single {
        val tasksPreferences = get<TasksPreferences>()
        GoogleAccountManager(
            context = androidContext(),
            authErrors = object : GoogleAccountManager.AuthErrorNotifications {
                override suspend fun alreadyNotified(account: String, scope: String) =
                    tasksPreferences.get(notifiedKey(account, scope), false)

                override suspend fun setAlreadyNotified(account: String, scope: String, notified: Boolean) =
                    tasksPreferences.set(notifiedKey(account, scope), notified)
            },
        )
    }
    single<GoogleTasksCredentialsProvider> { AccountManagerCredentials(get()) }
    single { GoogleAccountPicker(androidContext(), get()) }
    factory<SignInHandler>(PlatformSignInHandler) {
        AndroidSignInHandler(
            signIn = get(),
            microsoftSignIn = getOrNull(),
            googleAccountPicker = get(),
            caldavDao = get(),
            analytics = get<Reporting>(),
        )
    }
    single<Database> {
        val context = androidContext()
        val dbFile = context.getDatabasePath(Database.NAME)
        Room.databaseBuilder<Database>(
            context = context,
            name = dbFile.absolutePath,
        )
            .setBundledDriver()
            .addMigrations(*CommonMigrations.all)
            .addCallback(Database.CALLBACK)
            .build()
    }
    single {
        val context = androidContext()
        TasksPreferences(createDataStore(context))
    }
    factory<OkHttpClientFactory> {
        AndroidOkHttpClientFactory(
            context = androidContext(),
            userAgent = "${org.tasks.BuildConfig.APPLICATION_ID}/${org.tasks.BuildConfig.VERSION_NAME} (okhttp3) Android/${android.os.Build.VERSION.RELEASE}",
        )
    }
    single {
        PurchaseEntitlement(
            dataDir = androidContext().filesDir.absolutePath,
            encryption = get(),
            httpClientFactory = get(),
            serverEnvironment = get(),
            scope = get(),
            json = get(),
            syncAdapters = { get() },
        ).also { it.initialize() }
    }
    single<SubscriptionProvider> {
        val entitlement = EntitlementSubscriptionProvider(
            entitlement = get(),
            debugPro = get<TasksPreferences>().debugProFlow,
        )
        when (val store = getOrNull<StoreSubscriptionProvider>()) {
            null -> entitlement
            else -> CompositeSubscriptionProvider(listOf(store, entitlement))
        }
    }
    factory { FileStorage(androidContext().filesDir.absolutePath) }
    factory {
        EtebaseClientProvider(
            filesDir = androidContext().filesDir.absolutePath,
            encryption = get(),
            caldavDao = get(),
            httpClientFactory = get(),
        )
    }
    factory { OpenTaskDao(androidContext(), get()) }
    factory<OpenTasksSyncer> {
        OpenTasksSynchronizer(
            caldavDao = get(),
            taskDeleter = get(),
            refreshBroadcaster = get(),
            dirtyDao = get(),
            reporting = get(),
            iCalendar = get(),
            openTaskDao = get(),
        )
    }
    factoryOf(::VtodoCache)
}

private fun notifiedKey(account: String, scope: String) =
    booleanPreferencesKey("notified_oauth_error_${account}_$scope")
