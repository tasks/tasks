package org.tasks.injection

import android.app.NotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.todoroo.astrid.alarms.AlarmCalculator
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.service.AndroidCleanup
import com.todoroo.astrid.timers.TimerPlugin
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.tasks.LocalBroadcastManager
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.repeats.RepeatTaskHelper
import org.tasks.analytics.Firebase
import org.tasks.audio.SoundPlayer
import org.tasks.calendars.CalendarHelper
import org.tasks.service.TaskCompleter
import org.tasks.billing.PurchaseState
import org.tasks.viewmodel.DrawerViewModel
import org.tasks.broadcast.ComposeRefreshBroadcaster
import org.tasks.analytics.Reporting
import org.tasks.fcm.FcmTokenProvider
import org.tasks.fcm.PushTokenManager
import org.tasks.auth.TasksServerEnvironment
import org.tasks.billing.BillingClient
import org.tasks.billing.BillingClientImpl
import org.tasks.billing.Inventory
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.CaldavClientProvider
import org.tasks.etebase.EtebaseClientProvider
import org.tasks.etebase.EtebaseSynchronizer
import org.tasks.caldav.TasksBasicAuth
import org.tasks.feed.BlogFeedChecker
import org.tasks.http.HttpClientFactory
import org.tasks.caldav.FileStorage
import org.tasks.analytics.Analytics
import org.tasks.analytics.CrashReporting
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.caldav.VtodoCache
import org.tasks.compose.drawer.DrawerConfiguration
import org.tasks.R
import org.tasks.data.OpenTaskDao
import org.tasks.opentasks.OpenTaskContentObserver
import org.tasks.opentasks.OpenTasksSynchronizer
import org.tasks.data.TaskSaver
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.Astrid2ContentProviderDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.CompletionDao
import org.tasks.data.dao.DeletionDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.NotificationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.db.Database
import org.tasks.filters.FilterProvider
import org.tasks.filters.PreferenceDrawerConfiguration
import org.tasks.jobs.BackgroundWork
import org.tasks.jobs.WorkManager
import org.tasks.location.Geocoder
import org.tasks.location.LocationService
import org.tasks.notifications.Notifier
import org.tasks.pebble.PebbleMessageHandler
import org.tasks.pebble.PebbleRefresher
import org.tasks.pebble.PebbleService
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.Preferences
import org.tasks.preferences.TasksPreferences
import org.tasks.reminders.Random
import org.tasks.security.AndroidKeyStoreEncryption
import org.tasks.security.KeyStoreEncryption
import org.tasks.service.TaskCleanup
import org.tasks.service.TaskDeleter
import org.tasks.sync.SyncAdapters
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.themes.ColorProvider
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.tasklist.HeaderFormatter
import org.tasks.watch.WatchServiceLogic
import com.todoroo.astrid.service.TaskCreator
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationModule {
    @Provides
    @Singleton
    fun getJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    fun getLocale(): Locale {
        return AppCompatDelegate.getApplicationLocales()
            .toLanguageTags()
            .split(",")
            .firstOrNull { it.isNotBlank() }
            ?.let { Locale.forLanguageTag(it) }
            ?: Locale.getDefault()
    }

    @Provides
    @Singleton
    fun getReporting(firebase: Firebase): Reporting = firebase

    @Provides
    @Singleton
    fun getAnalytics(reporting: Reporting): Analytics = reporting

    @Provides
    @Singleton
    fun getCrashReporting(reporting: Reporting): CrashReporting = reporting

    @Provides
    @Singleton
    fun getNotificationDao(db: Database): NotificationDao = db.notificationDao()

    @Provides
    @Singleton
    fun getTagDataDao(db: Database): TagDataDao = db.tagDataDao()

    @Provides
    @Singleton
    fun getUserActivityDao(db: Database): UserActivityDao = db.userActivityDao()

    @Provides
    @Singleton
    fun getTaskAttachmentDao(db: Database): TaskAttachmentDao = db.taskAttachmentDao()

    @Provides
    @Singleton
    fun getTaskListMetadataDao(db: Database): TaskListMetadataDao = db.taskListMetadataDao()

    @Provides
    @Singleton
    fun getGoogleTaskDao(db: Database): GoogleTaskDao = db.googleTaskDao()

    @Provides
    @Singleton
    fun getAlarmDao(db: Database): AlarmDao = db.alarmDao()

    @Provides
    @Singleton
    fun getGeofenceDao(db: Database): LocationDao = db.locationDao()

    @Provides
    @Singleton
    fun getTagDao(db: Database): TagDao = db.tagDao()

    @Provides
    @Singleton
    fun getFilterDao(db: Database): FilterDao = db.filterDao()

    @Provides
    @Singleton
    fun getCaldavDao(db: Database): CaldavDao = db.caldavDao()

    @Provides
    @Singleton
    fun getTaskDao(db: Database): TaskDao = db.taskDao()

    @Provides
    @Singleton
    fun getDeletionDao(db: Database): DeletionDao = db.deletionDao()

    @Provides
    @Singleton
    fun getContentProviderDao(db: Database): Astrid2ContentProviderDao = db.contentProviderDao()

    @Provides
    @Singleton
    fun getUpgraderDao(db: Database) = db.upgraderDao()

    @Provides
    @Singleton
    fun getPrincipalDao(db: Database) = db.principalDao()

    @Provides
    @Singleton
    fun getCompletionDao(db: Database) = db.completionDao()

    @Provides
    fun getBillingClient(
        @ApplicationContext context: Context,
        inventory: Inventory,
        firebase: Firebase,
        workManager: WorkManager,
    ): BillingClient = BillingClientImpl(context, inventory, firebase, workManager)

    @Provides
    @Singleton
    fun provideSubscriptionProvider(
        inventory: Inventory,
        billingClient: BillingClient,
    ): org.tasks.billing.SubscriptionProvider =
        org.tasks.billing.AndroidSubscriptionProvider(inventory, billingClient)

    @Singleton
    @ApplicationScope
    @Provides
    fun providesCoroutineScope(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    @Provides
    fun providesNotificationManager(@ApplicationContext context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Provides
    fun providesDrawerConfiguration(preferences: Preferences): DrawerConfiguration =
        PreferenceDrawerConfiguration(preferences)

    @Provides
    fun providesFilterProvider(
        filterDao: FilterDao,
        tagDataDao: TagDataDao,
        caldavDao: CaldavDao,
        drawerConfiguration: DrawerConfiguration,
        locationDao: LocationDao,
        taskDao: TaskDao,
        tasksPreferences: TasksPreferences,
    ) = FilterProvider(
        filterDao = filterDao,
        tagDataDao = tagDataDao,
        caldavDao = caldavDao,
        configuration = drawerConfiguration,
        locationDao = locationDao,
        taskDao = taskDao,
        tasksPreferences = tasksPreferences,
    )

    @Provides
    @Singleton
    fun providesFileStorage(@ApplicationContext context: Context) =
        FileStorage(context.filesDir.absolutePath)

    @Provides
    @Singleton
    fun providesVtodoCache(caldavDao: CaldavDao, fileStorage: FileStorage) =
        VtodoCache(caldavDao, fileStorage)

    @Provides
    @Singleton
    fun providesKeyStoreEncryption(): KeyStoreEncryption = AndroidKeyStoreEncryption()

    @Provides
    fun providesBroadcastRefresh(localBroadcastManager: LocalBroadcastManager): RefreshBroadcaster =
        localBroadcastManager

    @Provides
    @Singleton
    fun providesComposeRefreshBroadcaster() = ComposeRefreshBroadcaster()

    @Provides
    fun providesPurchaseState(inventory: Inventory): PurchaseState = inventory

    @Provides
    fun providesCalendarHelper(gcalHelper: GCalHelper): CalendarHelper = gcalHelper

    @Provides
    fun providesRepeatTaskHelper(
        calendarHelper: CalendarHelper,
        alarmService: AlarmService,
        taskSaver: TaskSaver,
    ) = RepeatTaskHelper(calendarHelper, alarmService, taskSaver)

    @Provides
    fun providesSoundPlayer(
        @ApplicationContext context: Context,
        preferences: Preferences,
        notificationManager: org.tasks.notifications.NotificationManager,
    ): SoundPlayer = org.tasks.audio.AndroidSoundPlayer(context, preferences, notificationManager)

    @Provides
    fun providesTaskCompleter(
        taskDao: TaskDao,
        taskSaver: TaskSaver,
        notifier: Notifier,
        refreshBroadcaster: RefreshBroadcaster,
        repeatTaskHelper: RepeatTaskHelper,
        caldavDao: CaldavDao,
        calendarHelper: CalendarHelper,
        completionDao: CompletionDao,
        soundPlayer: SoundPlayer,
    ) = TaskCompleter(
        taskDao, taskSaver, notifier, refreshBroadcaster, repeatTaskHelper,
        caldavDao, calendarHelper, completionDao, soundPlayer,
    )

    @Provides
    @Singleton
    fun providesDrawerViewModel(
        filterProvider: FilterProvider,
        taskDao: TaskDao,
        caldavDao: CaldavDao,
        tasksPreferences: TasksPreferences,
        purchaseState: PurchaseState,
        composeRefreshBroadcaster: ComposeRefreshBroadcaster,
    ) = DrawerViewModel(
        filterProvider = filterProvider,
        taskDao = taskDao,
        caldavDao = caldavDao,
        tasksPreferences = tasksPreferences,
        purchaseState = purchaseState,
        refreshFlow = composeRefreshBroadcaster.refreshes,
    )

    @Provides
    fun providesEtebaseSynchronizer(
        caldavDao: CaldavDao,
        refreshBroadcaster: RefreshBroadcaster,
        taskDeleter: TaskDeleter,
        clientProvider: EtebaseClientProvider,
        iCal: org.tasks.caldav.iCalendar,
        vtodoCache: VtodoCache,
        reporting: Reporting,
    ): EtebaseSynchronizer = EtebaseSynchronizer(
        caldavDao = caldavDao,
        refreshBroadcaster = refreshBroadcaster,
        taskDeleter = taskDeleter,
        clientProvider = clientProvider,
        iCal = iCal,
        vtodoCache = vtodoCache,
        reporting = reporting,
    )

    @Provides
    fun providesEtebaseClientProvider(
        @ApplicationContext context: Context,
        encryption: KeyStoreEncryption,
        caldavDao: CaldavDao,
        httpClientFactory: HttpClientFactory,
    ): EtebaseClientProvider = EtebaseClientProvider(
        filesDir = context.filesDir.absolutePath,
        encryption = encryption,
        caldavDao = caldavDao,
        httpClientFactory = httpClientFactory,
    )

    @Provides
    fun providesOpenTaskDao(
        @ApplicationContext context: Context,
        caldavDao: CaldavDao,
    ): OpenTaskDao = OpenTaskDao(
        context = context,
        caldavDao = caldavDao,
    )

    @Provides
    fun providesOpenTasksSynchronizer(
        caldavDao: CaldavDao,
        taskDeleter: TaskDeleter,
        refreshBroadcaster: RefreshBroadcaster,
        taskDao: TaskDao,
        reporting: Reporting,
        iCal: org.tasks.caldav.iCalendar,
        openTaskDao: OpenTaskDao,
    ): OpenTasksSynchronizer = OpenTasksSynchronizer(
        caldavDao = caldavDao,
        taskDeleter = taskDeleter,
        refreshBroadcaster = refreshBroadcaster,
        taskDao = taskDao,
        reporting = reporting,
        iCalendar = iCal,
        openTaskDao = openTaskDao,
    )

    @Provides
    fun providesOpenTaskContentObserver(
        @ApplicationContext context: Context,
        syncAdapters: SyncAdapters,
        tasksPreferences: TasksPreferences,
    ): OpenTaskContentObserver = OpenTaskContentObserver(
        context = context,
        syncAdapters = syncAdapters,
        tasksPreferences = tasksPreferences,
    )

    @Provides
    fun providesCaldavClientProvider(
        encryption: KeyStoreEncryption,
        tasksPreferences: TasksPreferences,
        environment: TasksServerEnvironment,
        httpClientFactory: HttpClientFactory,
        tokenProvider: FcmTokenProvider,
        inventory: Inventory,
    ): CaldavClientProvider = CaldavClientProvider(
        encryption = encryption,
        tasksPreferences = tasksPreferences,
        environment = environment,
        httpClientFactory = httpClientFactory,
        tokenProvider = tokenProvider,
        subscriptionProvider = {
            inventory.subscription.value?.let {
                TasksBasicAuth.SubscriptionInfo(it.sku, it.purchaseToken)
            }
        },
    )

    @Provides
    @Singleton
    fun providesNotifier(notificationManager: org.tasks.notifications.NotificationManager): Notifier =
        notificationManager

    @Provides
    @Singleton
    fun providesAppPreferences(preferences: Preferences): AppPreferences =
        preferences

    @Provides
    fun providesAlarmCalculator(preferences: AppPreferences): AlarmCalculator =
        AlarmCalculator(Random(), runBlocking { preferences.defaultDueTime() })

    @Provides
    fun providesAlarmService(
        alarmDao: AlarmDao,
        taskDao: TaskDao,
        refreshBroadcaster: RefreshBroadcaster,
        notifier: Notifier,
        alarmCalculator: AlarmCalculator,
        preferences: AppPreferences,
    ): AlarmService =
        AlarmService(alarmDao, taskDao, refreshBroadcaster, notifier, alarmCalculator, preferences)

    @Provides
    @Singleton
    fun providesBackgroundWork(workManager: WorkManager): BackgroundWork = workManager

    @Provides
    @Singleton
    fun providesBlogFeedChecker(
        httpClientFactory: HttpClientFactory,
        tasksPreferences: TasksPreferences,
        appPreferences: AppPreferences,
        crashReporting: CrashReporting,
    ) = BlogFeedChecker(httpClientFactory, tasksPreferences, appPreferences, crashReporting)

    @Provides
    @Singleton
    fun providesTaskSaver(
        taskDao: TaskDao,
        refreshBroadcaster: RefreshBroadcaster,
        notifier: Notifier,
        locationService: LocationService,
        timerPlugin: TimerPlugin,
        syncAdapters: SyncAdapters,
        backgroundWork: BackgroundWork,
    ) = TaskSaver(taskDao, refreshBroadcaster, notifier, locationService, timerPlugin, syncAdapters, backgroundWork)

    @Provides
    fun providesTimerPlugin(
        notifier: Notifier,
        taskDao: TaskDao,
    ) = TimerPlugin(notifier, taskDao)

    @Provides
    fun providesCaldavSynchronizer(
        caldavDao: CaldavDao,
        taskDao: TaskDao,
        refreshBroadcaster: RefreshBroadcaster,
        taskDeleter: org.tasks.service.TaskDeleter,
        reporting: org.tasks.analytics.Reporting,
        provider: org.tasks.caldav.CaldavClientProvider,
        iCal: org.tasks.caldav.iCalendar,
        principalDao: org.tasks.data.dao.PrincipalDao,
        vtodoCache: VtodoCache,
        accountDataRepository: org.tasks.caldav.TasksAccountDataRepository,
    ) = org.tasks.caldav.CaldavSynchronizer(
        caldavDao, taskDao, refreshBroadcaster, taskDeleter, reporting,
        provider, iCal, principalDao, vtodoCache, accountDataRepository,
    )

    @Provides
    fun providesICalendar(
        tagDataDao: TagDataDao,
        preferences: AppPreferences,
        locationDao: LocationDao,
        geocoder: Geocoder,
        locationService: LocationService,
        tagDao: TagDao,
        taskDao: TaskDao,
        taskSaver: TaskSaver,
        caldavDao: CaldavDao,
        alarmDao: AlarmDao,
        alarmService: AlarmService,
        vtodoCache: VtodoCache,
        notifier: Notifier,
    ) = org.tasks.caldav.iCalendar(
        tagDataDao, preferences, locationDao, geocoder, locationService,
        tagDao, taskDao, taskSaver, caldavDao, alarmDao, alarmService, vtodoCache, notifier,
    )

    @Provides
    @Singleton
    fun providesSyncAdapters(
        backgroundWork: BackgroundWork,
        caldavDao: CaldavDao,
        googleTaskDao: GoogleTaskDao,
        openTaskDao: OpenTaskDao,
        tasksPreferences: TasksPreferences,
        refreshBroadcaster: RefreshBroadcaster,
    ) = SyncAdapters(
        backgroundWork = backgroundWork,
        caldavDao = caldavDao,
        googleTaskDao = googleTaskDao,
        openTaskSyncCheck = { openTaskDao.shouldSync() },
        tasksPreferences = tasksPreferences,
        refreshBroadcaster = refreshBroadcaster,
        coroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
    )

    @Provides
    fun providesFcmTokenProvider(firebase: Firebase): FcmTokenProvider = firebase

    @Provides
    @Singleton
    fun providesPushTokenManager(
        tokenProvider: FcmTokenProvider,
        caldavDao: CaldavDao,
        caldavClientProvider: CaldavClientProvider,
        @ApplicationScope scope: CoroutineScope,
    ) = PushTokenManager(tokenProvider, caldavDao, caldavClientProvider, scope)

    @Provides
    fun providesTaskCleanup(impl: AndroidCleanup): TaskCleanup = impl

    @Provides
    fun providesTaskDeleter(
        deletionDao: DeletionDao,
        taskDao: TaskDao,
        refreshBroadcaster: RefreshBroadcaster,
        vtodoCache: VtodoCache,
        tasksPreferences: TasksPreferences,
        taskCleanup: TaskCleanup,
    ) = TaskDeleter(deletionDao, taskDao, refreshBroadcaster, vtodoCache, tasksPreferences, taskCleanup)

    @Provides
    fun providesTaskMigrator(
        clientProvider: org.tasks.caldav.CaldavClientProvider,
        caldavDao: CaldavDao,
        syncAdapters: org.tasks.sync.SyncAdapters,
        taskDeleter: TaskDeleter,
    ) = org.tasks.service.TaskMigrator(clientProvider, caldavDao, syncAdapters, taskDeleter)

    @Provides
    @Singleton
    fun providesTasksServerEnvironment(
        tasksPreferences: TasksPreferences,
    ) = TasksServerEnvironment(
        tasksPreferences = tasksPreferences,
    )

    @Provides
    @Singleton
    fun providesTasksAccountDataRepository(
        provider: CaldavClientProvider,
        caldavDao: CaldavDao,
        tasksPreferences: TasksPreferences,
    ) = TasksAccountDataRepository(provider, caldavDao, tasksPreferences)

    @Provides
    fun providesPebbleRefresher(
        @ApplicationContext context: Context,
    ) = PebbleRefresher(context)

    @Provides
    @Singleton
    fun providesHeaderFormatter(caldavDao: CaldavDao) = HeaderFormatter(caldavDao)

    @Provides
    @Singleton
    fun providesChipDataProvider(
        caldavDao: CaldavDao,
        tagDataDao: TagDataDao,
        refreshBroadcaster: RefreshBroadcaster,
    ) = ChipDataProvider(caldavDao, tagDataDao, refreshBroadcaster)

    @Provides
    fun providesWatchServiceLogic(
        taskDao: TaskDao,
        taskSaver: TaskSaver,
        preferences: Preferences,
        taskCompleter: TaskCompleter,
        headerFormatter: HeaderFormatter,
        analytics: Analytics,
        filterProvider: FilterProvider,
        purchaseState: PurchaseState,
        colorProvider: ColorProvider,
        defaultFilterProvider: DefaultFilterProvider,
        taskCreator: TaskCreator,
        @ApplicationContext context: Context,
    ) = WatchServiceLogic(
        taskDao = taskDao,
        taskSaver = taskSaver,
        appPreferences = preferences,
        taskCompleter = taskCompleter,
        headerFormatter = headerFormatter,
        analytics = analytics,
        filterProvider = filterProvider,
        purchaseState = purchaseState,
        colorProvider = colorProvider,
        defaultFilterProvider = defaultFilterProvider,
        taskCreator = taskCreator,
        context = context,
    )

    @Provides
    fun providesPebbleMessageHandler(
        watchServiceLogic: WatchServiceLogic,
        firebase: Firebase,
    ) = PebbleMessageHandler(
        watchService = watchServiceLogic,
        analytics = firebase,
    )

    @Provides
    @Singleton
    fun providesPebbleService(
        @ApplicationContext context: Context,
        messageHandler: PebbleMessageHandler,
        @ApplicationScope scope: CoroutineScope,
    ) = PebbleService(context, messageHandler, scope)
}
