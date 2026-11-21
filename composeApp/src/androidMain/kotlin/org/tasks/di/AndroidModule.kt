package org.tasks.di

import android.content.Intent
import android.net.Uri
import androidx.room3.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.tasks.analytics.Reporting
import org.tasks.auth.AndroidSignInHandler
import org.tasks.auth.OAuthRedirect
import org.tasks.auth.SchemeRedirect
import org.tasks.auth.SignInHandler
import org.tasks.backup.shared.TasksJsonExporter
import org.tasks.billing.CompositeSubscriptionProvider
import org.tasks.auth.LoopbackRedirect
import org.tasks.extensions.bringTasksToForeground
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.GitHubSponsorVerifier
import org.tasks.billing.LoopbackGitHubSponsorClient
import org.tasks.billing.PurchaseEntitlement
import org.tasks.billing.StoreSubscriptionProvider
import org.tasks.billing.SubscriptionProvider
import org.tasks.billing.debugProFlow
import org.tasks.caldav.FileStorage
import org.tasks.caldav.VtodoCache
import org.tasks.data.OpenTaskDao
import org.tasks.etebase.EtebaseClientProvider
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
import org.tasks.viewmodel.BackupsViewModel

private fun notifierModule(): Module = module {
    factory<Notifier> {
        object : Notifier {
            override suspend fun cancel(id: Long, reason: CancelReason) = Unit
            override suspend fun cancel(ids: List<Long>, reason: CancelReason) = Unit
            override suspend fun cancelAll(reason: CancelReason) = Unit
            override fun triggerNotifications() = Unit
            override suspend fun updateTimerNotification() = Unit
        }
    }
}

private fun taskCleanupModule(): Module = module {
    factory<TaskCleanup> { object : TaskCleanup {} }
}

private fun reportingModule(): Module = module {
    factory<Reporting> {
        val tasksPreferences = get<TasksPreferences>()
        object : Reporting {
            override val tasksPreferences = tasksPreferences
            override val analyticsEnabled = false

            override fun logEvent(event: String, vararg params: Pair<String, Any>) = Unit
            override fun addTask(source: String) = Unit
            override fun completeTask(source: String) = Unit
            override fun setSku(sku: String?) = Unit
            override fun reportException(t: Throwable, fatal: Boolean) = Unit
        }
    }
}

private fun encryptionModule(): Module = module {
    single<KeyStoreEncryption> { AndroidKeyStoreEncryption() }
}

private fun githubSponsorModule(): Module = module {
    single<GitHubSponsorClient> {
        val context = androidContext()
        LoopbackGitHubSponsorClient(
            verify = get<GitHubSponsorVerifier>()::verify,
            redirect = LoopbackRedirect(
                path = LoopbackGitHubSponsorClient.LOOPBACK_CALLBACK_PATH,
                onRedirect = { context.bringTasksToForeground() },
                closeBrowserTab = false,
            ),
        )
    }
}

private fun signInModule(): Module = module {
    single { SchemeRedirect(androidContext()) }
    single<OAuthRedirect> { get<SchemeRedirect>() }
    single {
        get<OkHttpClientFactory>().tasksOAuthSignIn(
            caldavDao = get(),
            encryption = get<KeyStoreEncryption>(),
            serverEnvironment = get(),
            clientFactory = get(),
            purchaseClaim = get(),
            redirect = get(),
        )
    }
    factory<SignInHandler> {
        AndroidSignInHandler(
            signIn = get(),
            microsoftSignIn = getOrNull(),
        )
    }
}

private fun databaseModule(): Module = module {
    single<Database> {
        val context = androidContext()
        val dbFile = context.getDatabasePath(Database.NAME)
        Room
            .databaseBuilder<Database>(
                context = context,
                name = dbFile.absolutePath,
            ).setBundledDriver()
            .addMigrations(*CommonMigrations.all)
            .addCallback(Database.CALLBACK)
            .build()
    }
    single {
        val context = androidContext()
        TasksPreferences(createDataStore(context))
    }
}

private fun httpClientModule(): Module = module {
    factory<OkHttpClientFactory> {
        AndroidOkHttpClientFactory(
            context = androidContext(),
            userAgent = "${org.tasks.BuildConfig.APPLICATION_ID}/${org.tasks.BuildConfig.VERSION_NAME} (okhttp3) Android/${android.os.Build.VERSION.RELEASE}",
        )
    }
}

private fun purchaseEntitlementModule(): Module = module {
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
}

private fun subscriptionProviderModule(): Module = module {
    single<SubscriptionProvider> {
        val entitlement =
            EntitlementSubscriptionProvider(
                entitlement = get(),
                debugPro = get<TasksPreferences>().debugProFlow,
            )
        when (val store = getOrNull<StoreSubscriptionProvider>()) {
            null -> entitlement
            else -> CompositeSubscriptionProvider(listOf(store, entitlement))
        }
    }
}

private fun fileStorageModule(): Module = module {
    factory { FileStorage(androidContext().filesDir.absolutePath) }
}

private fun etebaseModule(): Module = module {
    factory {
        EtebaseClientProvider(
            filesDir = androidContext().filesDir.absolutePath,
            encryption = get(),
            caldavDao = get(),
            httpClientFactory = get(),
        )
    }
}

private fun openTaskDaoModule(): Module = module {
    factory { OpenTaskDao(androidContext(), get()) }
}

private fun openTasksModule(): Module = module {
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
}

private fun exportModule(): Module = module {
    factoryOf(::VtodoCache)
    single {
        TasksJsonExporter(
            taskDao = get(),
            alarmDao = get(),
            locationDao = get(),
            tagDao = get(),
            userActivityDao = get(),
            taskAttachmentDao = get(),
            caldavDao = get(),
            tagDataDao = get(),
            filterDao = get(),
            taskListMetadataDao = get(),
            vtodoCache = get(),
            json = get(),
        )
    }
}

private fun viewModelModule(): Module = module {
    viewModelOf(::BackupsViewModel)
}

actual fun platformModule(): Module =
    module {
        includes(flavorModule)
        includes(
            notifierModule(),
            taskCleanupModule(),
            reportingModule(),
            encryptionModule(),
            githubSponsorModule(),
            signInModule(),
            databaseModule(),
            httpClientModule(),
            purchaseEntitlementModule(),
            subscriptionProviderModule(),
            fileStorageModule(),
            etebaseModule(),
            openTaskDaoModule(),
            openTasksModule(),
            exportModule(),
            viewModelModule(),
        )
    }