package org.tasks.di

import com.todoroo.astrid.alarms.AlarmCalculator
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.repeats.RepeatTaskHelper
import com.todoroo.astrid.timers.TimerPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlinx.serialization.json.Json
import org.tasks.audio.SoundPlayer
import org.tasks.broadcast.ComposeRefreshBroadcaster
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.CaldavSynchronizer
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.caldav.iCalendar
import org.tasks.calendars.CalendarHelper
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.data.MergedGeofence
import org.tasks.data.TaskSaver
import org.tasks.data.db.Database
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.TaskCreator
import org.tasks.data.TaskMover
import org.tasks.data.getLocalList
import org.tasks.etebase.EtebaseSynchronizer
import org.tasks.googleapis.DefaultListProvider
import org.tasks.googleapis.DesktopGoogleTasksSynchronizer
import org.tasks.opentasks.OpenTasksSyncer
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.filters.FilterProvider
import org.tasks.jobs.BackgroundWork
import org.tasks.location.Geocoder
import org.tasks.location.LocationService
import org.tasks.location.MapPosition
import org.tasks.notifications.CancelReason
import org.tasks.notifications.Notifier
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DataStoreQueryPreferences
import org.tasks.preferences.QueryPreferences
import org.tasks.preferences.TasksPreferences
import org.tasks.reminders.Random
import org.tasks.service.TaskCleanup
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter
import org.tasks.service.TaskMigrator
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.tasklist.HeaderFormatter
import org.tasks.compose.accounts.AddAccountViewModel
import org.tasks.viewmodel.AppViewModel
import org.tasks.viewmodel.CaldavAccountSettingsViewModel
import org.tasks.viewmodel.CaldavCalendarSettingsViewModel
import org.tasks.viewmodel.EtebaseAccountSettingsViewModel
import org.tasks.viewmodel.EtebaseCalendarSettingsViewModel
import org.tasks.viewmodel.GoogleTaskListSettingsViewModel
import org.tasks.viewmodel.GoogleTasksAccountViewModel
import org.tasks.viewmodel.LocalListSettingsViewModel
import org.tasks.viewmodel.OpenTaskAccountViewModel
import org.tasks.viewmodel.DrawerViewModel
import org.tasks.viewmodel.FilterPickerViewModel
import org.tasks.viewmodel.SortSettingsViewModel
import org.tasks.viewmodel.TaskEditViewModel
import org.tasks.viewmodel.LocalAccountViewModel
import org.tasks.viewmodel.HelpAndFeedbackViewModel
import org.tasks.viewmodel.MainSettingsViewModel
import org.tasks.viewmodel.ProCardViewModel
import org.tasks.viewmodel.TasksAccountViewModel
import org.tasks.viewmodel.TaskListViewModel

val commonModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { Json { ignoreUnknownKeys = true } }

    // DAOs - singletons (from Database singleton)
    single { get<Database>().caldavDao() }
    single { get<Database>().taskDao() }
    single { get<Database>().tagDataDao() }
    single { get<Database>().tagDao() }
    single { get<Database>().alarmDao() }
    single { get<Database>().locationDao() }
    single { get<Database>().filterDao() }
    single { get<Database>().notificationDao() }
    single { get<Database>().googleTaskDao() }
    single { get<Database>().deletionDao() }
    single { get<Database>().contentProviderDao() }
    single { get<Database>().upgraderDao() }
    single { get<Database>().principalDao() }
    single { get<Database>().completionDao() }
    single { get<Database>().userActivityDao() }
    single { get<Database>().taskAttachmentDao() }
    single { get<Database>().taskListMetadataDao() }

    // No-op implementations
    single { ComposeRefreshBroadcaster() }
    factory<RefreshBroadcaster> { get<ComposeRefreshBroadcaster>() }
    factory<Notifier> {
        object : Notifier {
            override suspend fun cancel(id: Long, reason: CancelReason) {}
            override suspend fun cancel(ids: List<Long>, reason: CancelReason) {}
            override fun triggerNotifications() {}
            override suspend fun updateTimerNotification() {}
        }
    }
    factory<LocationService> {
        object : LocationService {
            override val locationDao = get<org.tasks.data.dao.LocationDao>()
            override val appPreferences = get<AppPreferences>()
            override suspend fun currentLocation(): MapPosition? = null
            override fun addGeofences(geofence: MergedGeofence) {}
            override fun removeGeofences(place: Place) {}
        }
    }
    factory<Geocoder> {
        object : Geocoder {
            override suspend fun reverseGeocode(mapPosition: MapPosition): Place? = null
        }
    }
    factory<AppPreferences> {
        val tasksPreferences = get<TasksPreferences>()
        object : AppPreferences {
            override suspend fun getInstallVersion() =
                tasksPreferences.get(TasksPreferences.installVersion, 0)
            override suspend fun setInstallVersion(value: Int) =
                tasksPreferences.set(TasksPreferences.installVersion, value)
            override suspend fun getInstallDate() =
                tasksPreferences.get(TasksPreferences.installDate, 0L)
            override suspend fun setInstallDate(value: Long) =
                tasksPreferences.set(TasksPreferences.installDate, value)
            override suspend fun getDeviceInstallVersion() =
                tasksPreferences.get(TasksPreferences.deviceInstallVersion, 0)
            override suspend fun setDeviceInstallVersion(value: Int) =
                tasksPreferences.set(TasksPreferences.deviceInstallVersion, value)
            override suspend fun isDefaultDueTimeEnabled() = false
            override suspend fun defaultLocationReminder() = 0
            override suspend fun defaultAlarms() = emptyList<Alarm>()
            override suspend fun defaultRandomHours() = 0
            override suspend fun defaultRingMode() = 0
            override suspend fun defaultDueTime() = 0
            override suspend fun defaultPriority() = 0
            override suspend fun isCurrentlyQuietHours() = false
            override suspend fun adjustForQuietHours(time: Long) = time
        }
    }
    factory<TaskCleanup> { object : TaskCleanup {} }
    factory<CalendarHelper> { object : CalendarHelper {} }
    factory<SoundPlayer> { object : SoundPlayer {} }
    factory<org.tasks.compose.drawer.DrawerConfiguration> {
        object : org.tasks.compose.drawer.DrawerConfiguration {
            override val canCreateFilters: Boolean get() = false
            override val canCreateTags: Boolean get() = false
            override val canCreatePlaces: Boolean get() = false
        }
    }
    single<org.tasks.billing.PurchaseState> {
        val caldavDao = get<org.tasks.data.dao.CaldavDao>()
        val subscriptionProvider = get<org.tasks.billing.SubscriptionProvider>()
        val _hasTasksAccount = MutableStateFlow(false)
        val _hasSubscription = MutableStateFlow(false)
        val _hasTasksSubscription = MutableStateFlow(false)
        get<CoroutineScope>().launch {
            caldavDao.watchAccounts()
                .collect { accounts ->
                    _hasTasksAccount.value = accounts.any { it.isTasksOrg }
                }
        }
        get<CoroutineScope>().launch {
            subscriptionProvider.subscription.collect { sub ->
                _hasSubscription.value = sub != null
                _hasTasksSubscription.value = sub?.isTasksSubscription == true
            }
        }
        object : org.tasks.billing.PurchaseState {
            override val hasTasksAccount: Boolean get() = _hasTasksAccount.value
            override val hasPro: Boolean get() = hasTasksAccount || _hasSubscription.value
            override val hasTasksSubscription: Boolean
                get() = _hasTasksSubscription.value || hasTasksAccount
        }
    }

    // Stateful singletons
    single<BackgroundWork> {
        val scope = get<CoroutineScope>()
        val mutex = kotlinx.coroutines.sync.Mutex()
        val pending = java.util.concurrent.atomic.AtomicBoolean(false)
        object : BackgroundWork {
            override fun updateCalendar(task: Task) {}
            override suspend fun scheduleRefresh(timestamp: Long) {}
            override suspend fun scheduleBlogFeedCheck() {}
            override fun migrateLocalTasks(
                localAccount: CaldavAccount,
                tasksAccount: CaldavAccount,
            ) {
                scope.launch {
                    get<TaskMigrator>().migrateLocalTasks(localAccount, tasksAccount)
                }
            }
            override suspend fun sync(source: SyncSource) {
                scope.launch {
                    if (!mutex.tryLock()) {
                        pending.set(true)
                        return@launch
                    }
                    try {
                        do {
                            pending.set(false)
                            val caldavSynchronizer = get<CaldavSynchronizer>()
                            val etebaseSynchronizer = get<EtebaseSynchronizer>()
                            val caldavDao = get<org.tasks.data.dao.CaldavDao>()
                            val hasPro = get<org.tasks.billing.PurchaseState>().hasPro
                            caldavDao.getAccounts(TYPE_CALDAV, TYPE_TASKS).forEach { account ->
                                caldavSynchronizer.sync(account, hasPro = hasPro)
                            }
                            caldavDao.getAccounts(TYPE_ETEBASE).forEach { account ->
                                etebaseSynchronizer.sync(account, hasPro = hasPro)
                            }
                            caldavDao.getAccounts(TYPE_GOOGLE_TASKS).forEach { account ->
                                get<DesktopGoogleTasksSynchronizer>().sync(account)
                            }
                            get<OpenTasksSyncer>().sync(hasPro = hasPro)
                        } while (pending.getAndSet(false))
                    } finally {
                        mutex.unlock()
                    }
                }
            }
        }
    }
    single { SyncAdapters(get(), get(), { false }, get(), get(), Dispatchers.IO) }
    singleOf(::TasksAccountDataRepository)

    // Stateless factories
    factory<CaldavClientProvider> {
        CaldavClientProvider(
            encryption = get(),
            tasksPreferences = get(),
            environment = get(),
            httpClientFactory = get(),
            tokenProvider = getOrNull(),
        )
    }
    factory { AlarmCalculator(Random(), 0) }
    factoryOf(::AlarmService)
    factory { RepeatTaskHelper(get(), get(), get()) }
    factory { TaskCompleter(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factoryOf(::TimerPlugin)
    factoryOf(::TaskDeleter)
    factoryOf(::TaskMigrator)
    factoryOf(::TaskSaver)
    factoryOf(::TaskMover)
    factoryOf(::iCalendar)
    factoryOf(::CaldavSynchronizer)
    factoryOf(::EtebaseSynchronizer)
    factory {
        DesktopGoogleTasksSynchronizer(
            caldavDao = get(),
            taskDao = get(),
            taskSaver = get(),
            reporting = get(),
            googleTaskDao = get(),
            defaultListProvider = get(),
            refreshBroadcaster = get(),
            taskDeleter = get(),
            alarmDao = get(),
            appPreferences = get(),
            repeatTaskHelper = get(),
            encryption = get(),
            createTask = { TaskCreator().createBlankTask() },
            proxyAuthProvider = get(),
        )
    }
    factory<DefaultListProvider> {
        val caldavDao = get<org.tasks.data.dao.CaldavDao>()
        object : DefaultListProvider {
            override suspend fun getDefaultList(): org.tasks.filters.CaldavFilter {
                val calendar = caldavDao.getCalendars()
                    .filterNot { it.readOnly() }
                    .firstOrNull()
                    ?.let { list ->
                        list.account
                            ?.let { caldavDao.getAccountByUuid(it) }
                            ?.let { account ->
                                org.tasks.filters.CaldavFilter(calendar = list, account = account)
                            }
                    }
                if (calendar != null) return calendar
                val localList = caldavDao.getLocalList()
                val localAccount = caldavDao.getAccountByUuid(localList.account!!)!!
                return org.tasks.filters.CaldavFilter(
                    calendar = localList,
                    account = localAccount,
                )
            }
            override suspend fun clearDefaultList() {}
        }
    }
    factory { FilterProvider(get(), get(), get(), get(), get(), get(), get()) }
    singleOf(::HeaderFormatter)
    singleOf(::ChipDataProvider)

    // ViewModels
    viewModelOf(::AppViewModel)
    viewModelOf(::AddAccountViewModel)
    viewModel {
        DrawerViewModel(
            filterProvider = get(),
            taskDao = get(),
            caldavDao = get(),
            tasksPreferences = get(),
            purchaseState = get(),
            refreshFlow = get<ComposeRefreshBroadcaster>().refreshes,
        )
    }
    viewModel { params ->
        FilterPickerViewModel(
            filterProvider = get(),
            caldavDao = get(),
            tasksPreferences = get(),
            purchaseState = get(),
            refreshBroadcaster = get<ComposeRefreshBroadcaster>(),
            listsOnly = params.get(),
            refreshFlow = get<ComposeRefreshBroadcaster>().refreshes,
        )
    }
    single<QueryPreferences> { DataStoreQueryPreferences(get()) }
    viewModel {
        TaskListViewModel(
            taskDao = get(),
            taskDeleter = get(),
            deletionDao = get(),
            taskSaver = get(),
            taskCompleter = get(),
            tasksPreferences = get(),
            queryPreferences = get(),
            refreshFlow = get<ComposeRefreshBroadcaster>().refreshes,
        )
    }
    viewModel {
        TaskEditViewModel(
            taskDao = get(),
            taskSaver = get(),
            caldavDao = get(),
            taskMover = get(),
        )
    }
    viewModel {
        SortSettingsViewModel(
            preferences = get(),
            reporting = get(),
            refreshBroadcaster = get(),
        )
    }
    viewModel {
        MainSettingsViewModel(
            platformConfiguration = get(),
        )
    }
    viewModel {
        HelpAndFeedbackViewModel(
            reporting = get(),
            tasksPreferences = get(),
            platformConfiguration = get(),
            purchaseState = get(),
        )
    }
    viewModel {
        LocalAccountViewModel(
            caldavDao = get(),
            taskDeleter = get(),
            backgroundWork = get(),
            purchaseState = get(),
        )
    }
    viewModel {
        OpenTaskAccountViewModel(
            caldavDao = get(),
        )
    }
    viewModel {
        TasksAccountViewModel(
            provider = get(),
            reporting = get(),
            accountDataRepository = get(),
            caldavDao = get(),
            principalDao = get(),
            backgroundWork = get(),
            pushTokenManager = get(),
            taskDeleter = get(),
            tasksPreferences = get(),
            subscriptionProvider = get(),
            caldavUrl = get<org.tasks.auth.TasksServerEnvironment>().caldavUrl,
        )
    }
    viewModel {
        GoogleTasksAccountViewModel(
            caldavDao = get(),
            taskDeleter = get(),
        )
    }
    viewModel {
        CaldavAccountSettingsViewModel(
            caldavDao = get(),
            caldavClientProvider = get(),
            encryption = get(),
            taskDeleter = get(),
            backgroundWork = get(),
            reporting = get(),
        )
    }
    viewModel { params ->
        CaldavCalendarSettingsViewModel(
            caldavDao = get(),
            caldavClientProvider = get(),
            principalDao = get(),
            taskDeleter = get(),
            syncAdapters = get(),
            reporting = get(),
            purchaseState = get(),
            isDark = params.get(),
            account = params.get(),
            calendar = params.get(),
        )
    }
    viewModel { params ->
        GoogleTaskListSettingsViewModel(
            caldavDao = get(),
            taskDeleter = get(),
            reporting = get(),
            purchaseState = get(),
            invokerFactory = { account ->
                org.tasks.googleapis.GtasksInvoker(
                    org.tasks.googleapis.GoogleTasksCredentialsAdapter(
                        account = account,
                        encryption = get(),
                        proxyAuthProvider = get(),
                        caldavDao = get(),
                    )
                )
            },
            isDark = params.get(),
            account = params.get(),
            calendar = params.get(),
        )
    }
    viewModel { params ->
        EtebaseCalendarSettingsViewModel(
            caldavDao = get(),
            clientProvider = get(),
            taskDeleter = get(),
            reporting = get(),
            purchaseState = get(),
            isDark = params.get(),
            account = params.get(),
            calendar = params.get(),
        )
    }
    viewModel { params ->
        LocalListSettingsViewModel(
            caldavDao = get(),
            taskDeleter = get(),
            reporting = get(),
            tasksPreferences = get(),
            purchaseState = get(),
            isDark = params.get(),
            account = params.get(),
            calendar = params.get(),
        )
    }
    viewModel {
        EtebaseAccountSettingsViewModel(
            caldavDao = get(),
            clientProvider = get(),
            encryption = get(),
            taskDeleter = get(),
            backgroundWork = get(),
            reporting = get(),
        )
    }
    viewModel {
        ProCardViewModel(
            caldavDao = get(),
            subscriptionProvider = get(),
            tasksPreferences = get(),
            accountDataRepository = get(),
            serverEnvironment = get(),
            platformConfiguration = get(),
        )
    }
}

expect fun platformModule(): Module
