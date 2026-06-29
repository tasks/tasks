package org.tasks.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.tasks.PlatformConfiguration
import org.tasks.analytics.PostHogReporting
import org.tasks.analytics.Reporting
import org.tasks.auth.DesktopOAuthFlow
import org.tasks.auth.DesktopSignInHandler
import org.tasks.auth.SignInHandler
import org.tasks.auth.TasksServerEnvironment
import org.tasks.billing.BillingProvider
import org.tasks.billing.DesktopEntitlement
import org.tasks.billing.DesktopLinkClient
import org.tasks.billing.EntitlementProvider
import org.tasks.billing.DesktopLinkClientImpl
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.GitHubSponsorClientImpl
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.FileStorage
import org.tasks.caldav.VtodoCache
import org.tasks.data.db.CommonMigrations
import org.tasks.data.db.Database
import org.tasks.etebase.EtebaseClientProvider
import org.tasks.opentasks.OpenTasksSyncer
import org.tasks.fcm.FcmTokenProvider
import org.tasks.fcm.PushTokenManager
import at.bitfire.cert4android.CertStore
import at.bitfire.cert4android.DesktopCertStore
import at.bitfire.cert4android.DesktopUserDecisionRegistry
import org.tasks.auth.TasksOAuthClient
import org.tasks.http.DesktopOkHttpClientFactory
import org.tasks.http.OkHttpClientFactory
import org.tasks.kmp.JvmBuildConfig
import org.tasks.kmp.createDataStore
import org.tasks.kmp.dataStoreFileName
import org.tasks.preferences.TasksPreferences
import org.tasks.security.DesktopKeyProvider
import org.tasks.security.KeyStoreEncryption
import org.tasks.sse.SseClient
import org.tasks.sse.SseTokenProvider
import java.io.File

private val appName: String =
    if (JvmBuildConfig.DEBUG) "Tasks.org.debug" else "Tasks.org"

private enum class Platform { MAC, WINDOWS, LINUX }

private fun platform(): Platform {
    val os = System.getProperty("os.name").lowercase()
    return when {
        "mac" in os || "darwin" in os -> Platform.MAC
        "win" in os -> Platform.WINDOWS
        else -> Platform.LINUX
    }
}

private val overrideDir: File? by lazy {
    val path = System.getProperty("tasks.dataDir")?.takeIf { it.isNotBlank() }
        ?: System.getenv("TASKS_DATA_DIR")?.takeIf { it.isNotBlank() }
    path?.let { File(it) }?.also {
        require(it.exists() || it.mkdirs()) { "Failed to create data directory: $it" }
        require(it.isDirectory) { "Data directory path is not a directory: $it" }
    }
}

val dataDir: File by lazy {
    overrideDir?.let { return@lazy it }
    val home = System.getProperty("user.home")
    val legacyDir = File(home, ".tasks.org")
    if (legacyDir.exists()) return@lazy legacyDir
    val dir = when (platform()) {
        Platform.MAC -> File(home, "Library/Application Support/$appName")
        Platform.WINDOWS ->
            File(System.getenv("LOCALAPPDATA") ?: "$home/AppData/Local", appName)
        Platform.LINUX -> {
            val xdgData = System.getenv("XDG_DATA_HOME") ?: "$home/.local/share"
            File(xdgData, appName.lowercase())
        }
    }
    dir.also { it.mkdirs() }
}

val logDir: File by lazy {
    overrideDir?.let { return@lazy File(it, "logs").also { d -> d.mkdirs() } }
    val home = System.getProperty("user.home")
    val dir = when (platform()) {
        Platform.MAC -> File(home, "Library/Logs/$appName")
        Platform.WINDOWS -> File(dataDir, "logs")
        Platform.LINUX -> {
            val xdgState = System.getenv("XDG_STATE_HOME") ?: "$home/.local/state"
            File(xdgState, "${appName.lowercase()}/logs")
        }
    }
    dir.also { it.mkdirs() }
}

actual fun platformModule(): Module = module {
    singleOf(::TasksServerEnvironment)
    single {
        PlatformConfiguration(
            versionCode = JvmBuildConfig.VERSION_CODE,
            billingProvider = BillingProvider.PADDLE,
            supportsCaldav = true,
            supportsEteSync = true,
            supportsGoogleTasks = true,
        )
    }
    single<Reporting> {
        PostHogReporting(
            apiKey = JvmBuildConfig.POSTHOG_KEY,
            dataDir = dataDir,
            tasksPreferences = get(),
        )
    }
    single { DesktopUserDecisionRegistry() }
    single<CertStore> { DesktopCertStore(dataDir = dataDir, userDecisionRegistry = get()) }
    factory<OkHttpClientFactory> { DesktopOkHttpClientFactory(certStore = get()) }
    factory {
        val httpClient = kotlinx.coroutines.runBlocking {
            get<OkHttpClientFactory>().newClient(foreground = true)
        }
        DesktopOAuthFlow(
            oauthClient = TasksOAuthClient(httpClient),
            serverEnvironment = get(),
        )
    }
    factoryOf(::DesktopSignInHandler) bind SignInHandler::class
    factory {
        org.tasks.googleapis.ProxyAuthProvider(
            caldavDao = get(),
            encryption = get(),
            jwtProvider = { get<DesktopEntitlement>().getJwt() },
        )
    }
    single {
        KeyStoreEncryption(
            DesktopKeyProvider(
                serviceName = "Tasks.org",
                accountName = "encryption-key",
                fallbackKeyFile = File(dataDir, ".key"),
            )
        )
    }
    single<Database> {
        val dbFile = File(dataDir, Database.NAME)
        Room.databaseBuilder<Database>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*CommonMigrations.all)
            .addCallback(Database.CALLBACK)
            .build()
    }
    single {
        val dataStoreFile = File(dataDir, dataStoreFileName)
        TasksPreferences(createDataStore { dataStoreFile.absolutePath })
    }
    factory {
        FileStorage(dataDir.absolutePath)
    }
    factory {
        EtebaseClientProvider(
            filesDir = dataDir.absolutePath,
            encryption = get(),
            caldavDao = get(),
            httpClientFactory = get(),
        )
    }
    factory<OpenTasksSyncer> {
        val caldavDao = get<org.tasks.data.dao.CaldavDao>()
        val refreshBroadcaster = get<org.tasks.broadcast.RefreshBroadcaster>()
        object : OpenTasksSyncer {
            override suspend fun sync(hasPro: Boolean) {
                caldavDao.getAccounts(org.tasks.data.entity.CaldavAccount.TYPE_OPENTASKS).forEach { account ->
                    account.error = "OpenTasks sync is not supported on desktop"
                    caldavDao.update(account)
                    refreshBroadcaster.broadcastRefresh()
                }
            }
        }
    }
    factoryOf(::VtodoCache)
    single {
        DesktopEntitlement(
            dataDir = dataDir,
            httpClientFactory = get(),
            serverEnvironment = get(),
            scope = get(),
            json = get(),
            encryption = get(),
        ).also { it.initialize() }
    }
    single<SubscriptionProvider> {
        val entitlement: DesktopEntitlement = get()
        object : SubscriptionProvider {
            override val subscription: Flow<SubscriptionProvider.SubscriptionInfo?> =
                kotlinx.coroutines.flow.combine(entitlement.hasPro, entitlement.sku, entitlement.provider) { hasPro, sku, provider ->
                    if (hasPro) {
                        val isMonthly = sku?.startsWith("monthly") == true
                        SubscriptionProvider.SubscriptionInfo(
                            sku = sku ?: "desktop_play",
                            isMonthly = isMonthly,
                            isTasksSubscription = isTasksSubscription(sku, isMonthly),
                            isGitHubSponsor = provider == EntitlementProvider.GITHUB_SPONSOR,
                        )
                    } else {
                        null
                    }
                }
            override suspend fun getFormattedPrice(sku: String): String? =
                entitlement.formattedPrice.first()
        }
    }
    single<DesktopLinkClient> {
        DesktopLinkClientImpl(
            httpClientFactory = get(),
            serverEnvironment = get(),
            desktopEntitlement = get(),
            json = get(),
        )
    }
    single<GitHubSponsorClient> {
        GitHubSponsorClientImpl(
            httpClientFactory = get(),
            serverEnvironment = get(),
            desktopEntitlement = get(),
            json = get(),
        )
    }
    single { SseTokenProvider() } bind FcmTokenProvider::class
    single {
        PushTokenManager(
            tokenProvider = get(),
            caldavDao = get(),
            caldavClientProvider = get(),
            scope = get(),
        )
    }
    single {
        SseClient(
            scope = get(),
            backgroundWork = get(),
            caldavDao = get(),
            encryption = get(),
            environment = get(),
            httpClientFactory = get(),
            tokenProvider = get(),
        )
    }
}

private val SKU_PATTERN = Regex("^(annual|monthly)_(\\d+)$")

private fun isTasksSubscription(sku: String?, isMonthly: Boolean): Boolean {
    if (sku == null) return false
    val match = SKU_PATTERN.matchEntire(sku) ?: return false
    val price = match.groupValues[2].toIntOrNull() ?: return false
    val effectivePrice = if (price == 499) 5 else price
    return if (isMonthly) effectivePrice >= 3 else effectivePrice >= 30
}
