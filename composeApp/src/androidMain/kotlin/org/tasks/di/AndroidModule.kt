package org.tasks.di

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.tasks.analytics.Reporting
import org.tasks.auth.AndroidSignInHandler
import org.tasks.auth.SignInHandler
import org.tasks.auth.TasksServerEnvironment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.FileStorage
import org.tasks.caldav.VtodoCache
import org.tasks.data.OpenTaskDao
import org.tasks.etebase.EtebaseClientProvider
import org.tasks.opentasks.OpenTasksSyncer
import org.tasks.opentasks.OpenTasksSynchronizer
import org.tasks.http.AndroidOkHttpClientFactory
import org.tasks.http.OkHttpClientFactory
import org.tasks.data.db.Database
import org.tasks.kmp.createDataStore
import org.tasks.preferences.TasksPreferences
import org.tasks.security.AndroidKeyStoreEncryption
import org.tasks.security.KeyStoreEncryption

actual fun platformModule(): Module = module {
    includes(flavorModule)
    singleOf(::TasksServerEnvironment)
    factory<Reporting> {
        object : Reporting {
            override fun logEvent(event: String, vararg params: Pair<String, Any>) {}
            override fun addTask(source: String) {}
            override fun completeTask(source: String) {}
            override fun identify(distinctId: String) {}
            override fun reportException(t: Throwable, fatal: Boolean) {}
        }
    }
    single<KeyStoreEncryption> { AndroidKeyStoreEncryption() }
    factory<SignInHandler> { AndroidSignInHandler(androidContext(), get(), get(), get(), get(), get()) }
    single<Database> {
        val context = androidContext()
        val dbFile = context.getDatabasePath(Database.NAME)
        Room.databaseBuilder<Database>(
            context = context,
            name = dbFile.absolutePath,
        ).build()
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
    single<SubscriptionProvider> {
        object : SubscriptionProvider {
            override val subscription: Flow<SubscriptionProvider.SubscriptionInfo?> = flowOf(null)
            override suspend fun getFormattedPrice(sku: String): String? = null
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
            taskDao = get(),
            reporting = get(),
            iCalendar = get(),
            openTaskDao = get(),
        )
    }
    factoryOf(::VtodoCache)
}
