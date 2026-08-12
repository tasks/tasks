package org.tasks.di

import co.touchlab.kermit.Logger
import com.todoroo.astrid.alarms.AlarmCalculator
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.repeats.RepeatTaskHelper
import com.todoroo.astrid.timers.TimerPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.tasks.analytics.Reporting
import org.tasks.audio.SoundPlayer
import org.tasks.broadcast.ComposeRefreshBroadcaster
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.CaldavSynchronizer
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.caldav.iCalendar
import org.tasks.calendars.CalendarHelper
import org.tasks.compose.accounts.AddAccountViewModel
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.data.MergedGeofence
import org.tasks.data.TaskCreator
import org.tasks.data.SubtaskTreeWriter
import org.tasks.data.SubtaskTreeRegistry
import org.tasks.data.TaskMover
import org.tasks.data.TaskSaver
import org.tasks.data.db.Database
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.data.getOrCreateDefaultListFilter
import org.tasks.etebase.EtebaseSynchronizer
import org.tasks.extensions.guarded
import org.tasks.filters.CaldavListCache
import org.tasks.filters.FilterProvider
import org.tasks.googleapis.DefaultListProvider
import org.tasks.googleapis.DesktopGoogleTasksSynchronizer
import org.tasks.sync.microsoft.MicrosoftSynchronizer
import org.tasks.jobs.BackgroundWork
import org.tasks.location.Geocoder
import org.tasks.location.LocationService
import org.tasks.location.MapPosition
import org.tasks.notifications.CancelReason
import org.tasks.notifications.Notifier
import org.tasks.opentasks.OpenTasksSyncer
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DEFAULT_ALARMS_JSON
import org.tasks.preferences.DataStoreQueryPreferences
import org.tasks.preferences.DatePickerPreferences
import org.tasks.preferences.NotificationSettings
import org.tasks.preferences.PreferencesSnapshot
import org.tasks.preferences.QueryPreferences
import org.tasks.preferences.TaskDefaultSettings
import org.tasks.preferences.TasksPreferences
import org.tasks.preferences.adjustForQuietHours
import org.tasks.preferences.isCurrentlyQuietHours
import org.tasks.preferences.toAlarmJson
import org.tasks.preferences.toAlarms
import org.tasks.reminders.Random
import org.tasks.reminders.ReminderControlSetViewModel
import org.tasks.repeats.CustomRecurrenceViewModel
import org.tasks.repeats.RepeatRuleToString
import org.tasks.service.TaskCleanup
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter
import org.tasks.service.TaskMigrator
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.tags.TagPickerViewModel
import org.tasks.tasklist.HeaderFormatter
import org.tasks.viewmodel.AppViewModel
import org.tasks.viewmodel.CaldavAccountSettingsViewModel
import org.tasks.viewmodel.CaldavCalendarSettingsViewModel
import org.tasks.viewmodel.DrawerViewModel
import org.tasks.viewmodel.EtebaseAccountSettingsViewModel
import org.tasks.viewmodel.EtebaseCalendarSettingsViewModel
import org.tasks.viewmodel.FilterPickerViewModel
import org.tasks.viewmodel.GoogleTaskListSettingsViewModel
import org.tasks.viewmodel.GoogleTasksAccountViewModel
import org.tasks.viewmodel.HelpAndFeedbackViewModel
import org.tasks.viewmodel.LocalAccountViewModel
import org.tasks.viewmodel.LocalListSettingsViewModel
import org.tasks.viewmodel.MicrosoftListSettingsViewModel
import org.tasks.viewmodel.MainSettingsViewModel
import org.tasks.viewmodel.NotificationsViewModel
import org.tasks.viewmodel.ReminderChange
import org.tasks.viewmodel.OpenTaskAccountViewModel
import org.tasks.viewmodel.ProCardViewModel
import org.tasks.viewmodel.SortSettingsViewModel
import org.tasks.viewmodel.TagSettingsViewModel
import org.tasks.viewmodel.TaskDefaultsViewModel
import org.tasks.TaskEditDestination
import org.tasks.viewmodel.PendingTaskSaves
import org.tasks.viewmodel.TaskEditViewModel
import org.tasks.viewmodel.TaskListViewModel
import org.tasks.viewmodel.TasksAccountViewModel
import java.util.Locale

private const val SYNC_TAG = "BackgroundWork"

val commonModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { PendingTaskSaves(get()) }
    single { org.tasks.TaskRequests() }
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
    single { get<Database>().dirtyDao() }

    // No-op implementations
    single { ComposeRefreshBroadcaster() }
    factory<RefreshBroadcaster> { get<ComposeRefreshBroadcaster>() }
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
            override suspend fun isDefaultDueTimeEnabled() =
                tasksPreferences.get(
                    TasksPreferences.defaultRemindersEnabled,
                    notificationDefaults.defaultRemindersEnabled
                )
            override suspend fun defaultLocationReminder() =
                tasksPreferences.get(
                    TasksPreferences.defaultLocationReminder,
                    taskSettingDefaults.defaultLocationReminder
                )
            override suspend fun defaultAlarms() =
                tasksPreferences
                    .get(TasksPreferences.defaultAlarms, DEFAULT_ALARMS_JSON)
                    .toAlarms()
            override suspend fun defaultRingMode() =
                tasksPreferences.get(TasksPreferences.defaultRingMode, taskSettingDefaults.defaultRingMode)
            override suspend fun defaultDueTime() =
                tasksPreferences.get(
                    TasksPreferences.defaultReminderTime,
                    notificationDefaults.defaultReminderTime
                )
            override suspend fun defaultPriority() =
                tasksPreferences.get(
                    TasksPreferences.defaultPriority,
                    taskSettingDefaults.defaultPriority
                )
            override suspend fun locationUpdateIntervalMinutes() =
                tasksPreferences.get(
                    TasksPreferences.locationUpdateInterval,
                    taskSettingDefaults.locationUpdateIntervalMinutes
                )
            override suspend fun addTasksToTop() =
                tasksPreferences.get(TasksPreferences.addTasksToTop, taskSettingDefaults.addTasksToTop)
            override suspend fun taskDefaults(): TaskDefaultSettings {
                val prefs = tasksPreferences.snapshot()
                return TaskDefaultSettings(
                    addTasksToTop = prefs.get(
                        TasksPreferences.addTasksToTop,
                        taskSettingDefaults.addTasksToTop
                    ),
                    defaultList = prefs.get(TasksPreferences.defaultList, "").orNull(),
                    defaultTags = prefs
                        .get(TasksPreferences.defaultTags, "")
                        .split(",")
                        .filter { it.isNotBlank() },
                    defaultPriority = prefs.get(
                        TasksPreferences.defaultPriority,
                        taskSettingDefaults.defaultPriority
                    ),
                    defaultHideUntil = prefs.get(
                        TasksPreferences.defaultHideUntil,
                        taskSettingDefaults.defaultHideUntil
                    ),
                    defaultDueDate = prefs.get(
                        TasksPreferences.defaultDueDate,
                        taskSettingDefaults.defaultDueDate
                    ),
                    defaultCalendar = prefs.get(TasksPreferences.defaultCalendar, "").orNull(),
                    defaultRecurrence = prefs.get(TasksPreferences.defaultRecurrence, "").orNull(),
                    defaultRecurrenceFrom = prefs.get(
                        TasksPreferences.defaultRecurrenceFrom,
                        taskSettingDefaults.defaultRecurrenceFrom
                    ),
                    defaultAlarms = prefs
                        .get(TasksPreferences.defaultAlarms, DEFAULT_ALARMS_JSON)
                        .toAlarms(),
                    defaultRingMode = prefs.get(
                        TasksPreferences.defaultRingMode,
                        taskSettingDefaults.defaultRingMode
                    ),
                    defaultLocation = prefs.get(TasksPreferences.defaultLocation, "").orNull(),
                    defaultLocationReminder = prefs.get(
                        TasksPreferences.defaultLocationReminder,
                        taskSettingDefaults.defaultLocationReminder
                    ),
                    locationUpdateIntervalMinutes = prefs.get(
                        TasksPreferences.locationUpdateInterval,
                        taskSettingDefaults.locationUpdateIntervalMinutes
                    ),
                )
            }
            override suspend fun setAddTasksToTop(value: Boolean) =
                tasksPreferences.set(TasksPreferences.addTasksToTop, value)
            override suspend fun setDefaultList(value: String?) =
                tasksPreferences.set(TasksPreferences.defaultList, value.orEmpty())
            override suspend fun setDefaultTags(value: List<String>) =
                tasksPreferences.set(TasksPreferences.defaultTags, value.joinToString(","))
            override suspend fun setDefaultPriority(value: Int) =
                tasksPreferences.set(TasksPreferences.defaultPriority, value)
            override suspend fun setDefaultHideUntil(value: Int) =
                tasksPreferences.set(TasksPreferences.defaultHideUntil, value)
            override suspend fun setDefaultDueDate(value: Int) =
                tasksPreferences.set(TasksPreferences.defaultDueDate, value)
            override suspend fun setDefaultCalendar(value: String?) =
                tasksPreferences.set(TasksPreferences.defaultCalendar, value.orEmpty())
            override suspend fun setDefaultRecurrence(value: String?) =
                tasksPreferences.set(TasksPreferences.defaultRecurrence, value.orEmpty())
            override suspend fun setDefaultRecurrenceFrom(value: Int) =
                tasksPreferences.set(TasksPreferences.defaultRecurrenceFrom, value)
            override suspend fun setDefaultAlarms(value: List<Alarm>) =
                tasksPreferences.set(TasksPreferences.defaultAlarms, value.toAlarmJson())
            override suspend fun setDefaultRingMode(value: Int) =
                tasksPreferences.set(TasksPreferences.defaultRingMode, value)
            override suspend fun setDefaultLocation(value: String?) =
                tasksPreferences.set(TasksPreferences.defaultLocation, value.orEmpty())
            override suspend fun setDefaultLocationReminder(value: Int) =
                tasksPreferences.set(TasksPreferences.defaultLocationReminder, value)
            override suspend fun setLocationUpdateIntervalMinutes(value: Int) =
                tasksPreferences.set(TasksPreferences.locationUpdateInterval, value)
            override suspend fun isCurrentlyQuietHours() =
                tasksPreferences.snapshot().notificationSettings().isCurrentlyQuietHours()
            override suspend fun adjustForQuietHours(time: Long) =
                tasksPreferences.snapshot().notificationSettings().adjustForQuietHours(time)
            override suspend fun notificationSettings() =
                tasksPreferences.snapshot().notificationSettings()
            override suspend fun setNotificationsEnabled(value: Boolean) =
                tasksPreferences.set(TasksPreferences.notificationsEnabled, value)
            override suspend fun setPersistentNotifications(value: Boolean) =
                tasksPreferences.set(TasksPreferences.persistentNotifications, value)
            override suspend fun setWearableNotifications(value: Boolean) =
                tasksPreferences.set(TasksPreferences.wearableNotifications, value)
            override suspend fun setBundleNotifications(value: Boolean) =
                tasksPreferences.set(TasksPreferences.bundleNotifications, value)
            override suspend fun setVoiceReminders(value: Boolean) =
                tasksPreferences.set(TasksPreferences.voiceReminders, value)
            override suspend fun setSwipeToSnoozeEnabled(value: Boolean) =
                tasksPreferences.set(TasksPreferences.swipeToSnoozeEnabled, value)
            override suspend fun setSwipeToSnoozeMinutes(value: Int) =
                tasksPreferences.set(TasksPreferences.swipeToSnoozeMinutes, value)
            override suspend fun setDefaultRemindersEnabled(value: Boolean) =
                tasksPreferences.set(TasksPreferences.defaultRemindersEnabled, value)
            override suspend fun setDefaultReminderTime(value: Int) =
                tasksPreferences.set(TasksPreferences.defaultReminderTime, value)
            override suspend fun setQuietHoursEnabled(value: Boolean) =
                tasksPreferences.set(TasksPreferences.quietHoursEnabled, value)
            override suspend fun setQuietHoursStart(value: Int) =
                tasksPreferences.set(TasksPreferences.quietHoursStart, value)
            override suspend fun setQuietHoursEnd(value: Int) =
                tasksPreferences.set(TasksPreferences.quietHoursEnd, value)
            // TODO: populate the remaining DatePickerPreferences fields
            override suspend fun datePickerPreferences() = DatePickerPreferences(
                datePickerInputMode = tasksPreferences.get(TasksPreferences.datePickerInputMode, false),
                timePickerInputMode = tasksPreferences.get(TasksPreferences.timePickerInputMode, false),
            )
            override suspend fun setDatePickerInputMode(value: Boolean) =
                tasksPreferences.set(TasksPreferences.datePickerInputMode, value)
            override suspend fun setTimePickerInputMode(value: Boolean) =
                tasksPreferences.set(TasksPreferences.timePickerInputMode, value)
        }
    }
    factory<TaskCleanup> { object : TaskCleanup {} }
    factory<CalendarHelper> { object : CalendarHelper {} }
    factory<SoundPlayer> { object : SoundPlayer {} }
    factory<org.tasks.compose.drawer.DrawerConfiguration> {
        object : org.tasks.compose.drawer.DrawerConfiguration {
            override val canCreateFilters: Boolean get() = false
            override val canCreateTags: Boolean get() = true
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
                            val subscriptionProvider = get<org.tasks.billing.SubscriptionProvider>()
                            val caldavAccounts = caldavDao.getAccounts(TYPE_CALDAV, TYPE_TASKS)
                            val hasTasksOrg = caldavAccounts.any { it.isTasksOrg }
                            if (!hasTasksOrg && !subscriptionProvider.awaitVerification()) {
                                Logger.e(tag = SYNC_TAG) {
                                    "Could not confirm subscription, syncing without pro"
                                }
                            }
                            val hasPro = hasTasksOrg ||
                                    subscriptionProvider.subscription.first() != null
                            val googleAndMicrosoftPro =
                                hasPro || !subscriptionProvider.googleAndMicrosoftRequirePro
                            caldavAccounts.forEach { account ->
                                caldavSynchronizer.sync(account, hasPro = hasPro)
                            }
                            caldavDao.getAccounts(TYPE_ETEBASE).forEach { account ->
                                etebaseSynchronizer.sync(account, hasPro = hasPro)
                            }
                            caldavDao.getAccounts(TYPE_GOOGLE_TASKS).forEach { account ->
                                get<DesktopGoogleTasksSynchronizer>()
                                    .sync(account, hasPro = googleAndMicrosoftPro)
                            }
                            val microsoftAccounts = caldavDao.getAccounts(TYPE_MICROSOFT)
                            if (microsoftAccounts.isNotEmpty()) {
                                val microsoftSynchronizer = get<MicrosoftSynchronizer>()
                                coroutineScope {
                                    microsoftAccounts.forEach { account ->
                                        launch {
                                            microsoftSynchronizer
                                                .sync(account, hasPro = googleAndMicrosoftPro)
                                        }
                                    }
                                }
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
    single { SyncAdapters(get(), get(), get(), { false }, get(), get(), Dispatchers.IO) }
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
    factory { AlarmCalculator(Random()) }
    factoryOf(::AlarmService)
    factory { RepeatTaskHelper(get(), get(), get()) }
    factory { TaskCompleter(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factoryOf(::TimerPlugin)
    factoryOf(::TaskDeleter)
    factoryOf(::TaskMigrator)
    factoryOf(::TaskSaver)
    factoryOf(::TaskMover)
    factoryOf(::SubtaskTreeWriter)
    single { SubtaskTreeRegistry() }
    factoryOf(::iCalendar)
    factoryOf(::CaldavSynchronizer)
    single {
        org.tasks.caldav.metadata.TagMetadataSync(
            caldavDao = get(),
            tagDataDao = get(),
            provider = get(),
            vtodoCache = get(),
            preferences = get(),
        )
    }
    factoryOf(::EtebaseSynchronizer)
    factory {
        DesktopGoogleTasksSynchronizer(
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
            encryption = get(),
            createTask = { TaskCreator().createBlankTask() },
            proxyAuthProvider = get(),
        )
    }
    factory<DefaultListProvider> {
        val caldavDao = get<org.tasks.data.dao.CaldavDao>()
        val tasksPreferences = get<TasksPreferences>()
        object : DefaultListProvider {
            override suspend fun getDefaultList(): org.tasks.filters.CaldavFilter =
                caldavDao.getOrCreateDefaultListFilter(
                    tasksPreferences.get(TasksPreferences.defaultList, "").orNull()
                )

            override suspend fun clearDefaultList() {
                tasksPreferences.set(TasksPreferences.defaultList, "")
            }
        }
    }
    factory { FilterProvider(get(), get(), get(), get(), get(), get(), get(), get()) }
    singleOf(::CaldavListCache)
    singleOf(::HeaderFormatter)
    singleOf(::ChipDataProvider)
    single { Locale.getDefault() }
    single { RepeatRuleToString(locale = get(), crashReporting = get<Reporting>()) }

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
            headerFormatter = get(),
            queryPreferences = get(),
            refreshFlow = get<ComposeRefreshBroadcaster>().refreshes,
        )
    }
    viewModel { params ->
        val destination = params.get<TaskEditDestination>()
        TaskEditViewModel(
            taskId = destination.taskId,
            remoteId = destination.remoteId,
            listId = destination.listId,
            tagUuid = destination.tagUuid,
            isSubtaskDraft = destination.isSubtaskDraft,
            taskDao = get(),
            taskSaver = get(),
            caldavDao = get(),
            taskMover = get(),
            tagDao = get(),
            tagDataDao = get(),
            alarmDao = get(),
            alarmService = get(),
            appPreferences = get(),
            externalScope = get(),
            pendingSaves = get(),
            taskCompleter = get(),
            taskDeleter = get(),
            treeRegistry = get(),
            subtaskWriter = get(),
            refreshFlow = get<ComposeRefreshBroadcaster>().refreshes,
        )
    }
    viewModel { ReminderControlSetViewModel() }
    viewModel {
        TagPickerViewModel(
            tagDataDao = get(),
            syncAdapters = get(),
        )
    }
    viewModel { params ->
        CustomRecurrenceViewModel(
            rrule = params.get<String>(),
            dueDate = params.get<Long>(),
            accountType = params.get<Int>(),
            locale = get(),
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
        val notifier = get<Notifier>()
        NotificationsViewModel(
            appPreferences = get(),
            platformConfiguration = get(),
            persistenceScope = get(),
            rescheduleNotifications = { change ->
                if (change == ReminderChange.OFF) {
                    guarded("CommonModule", "Failed to take down notifications", Unit) {
                        notifier.cancelAll(CancelReason.DISABLED)
                    }
                }
                notifier.triggerNotifications()
            },
        )
    }
    viewModel {
        TaskDefaultsViewModel(
            appPreferences = get(),
            platformConfiguration = get(),
            persistenceScope = get(),
            caldavDao = get(),
            tagDataDao = get(),
            locationDao = get(),
            repeatRuleToString = get(),
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
            reporting = get(),
            tagMetadataSync = get(),
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
        MicrosoftListSettingsViewModel(
            caldavDao = get(),
            taskDeleter = get(),
            reporting = get(),
            clientProvider = get(),
            purchaseState = get(),
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
    viewModel { params ->
        TagSettingsViewModel(
            tagDataDao = get(),
            refreshBroadcaster = get(),
            reporting = get(),
            purchaseState = get(),
            tagMetadataSync = get(),
            syncAdapters = get(),
            isDark = params.get(),
            tagData = params.get(),
        )
    }
    viewModel {
        EtebaseAccountSettingsViewModel(
            caldavDao = get(),
            clientProvider = get(),
            encryption = get(),
            taskDeleter = get(),
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

private val notificationDefaults = NotificationSettings()

private val taskSettingDefaults = TaskDefaultSettings()

private fun PreferencesSnapshot.notificationSettings() = NotificationSettings(
    notificationsEnabled = get(
        TasksPreferences.notificationsEnabled,
        notificationDefaults.notificationsEnabled
    ),
    persistentNotifications = get(
        TasksPreferences.persistentNotifications,
        notificationDefaults.persistentNotifications
    ),
    wearableNotifications = get(
        TasksPreferences.wearableNotifications,
        notificationDefaults.wearableNotifications
    ),
    bundleNotifications = get(
        TasksPreferences.bundleNotifications,
        notificationDefaults.bundleNotifications
    ),
    voiceReminders = get(TasksPreferences.voiceReminders, notificationDefaults.voiceReminders),
    swipeToSnoozeEnabled = get(
        TasksPreferences.swipeToSnoozeEnabled,
        notificationDefaults.swipeToSnoozeEnabled
    ),
    swipeToSnoozeMinutes = get(
        TasksPreferences.swipeToSnoozeMinutes,
        notificationDefaults.swipeToSnoozeMinutes
    ),
    defaultRemindersEnabled = get(
        TasksPreferences.defaultRemindersEnabled,
        notificationDefaults.defaultRemindersEnabled
    ),
    defaultReminderTime = get(
        TasksPreferences.defaultReminderTime,
        notificationDefaults.defaultReminderTime
    ),
    quietHoursEnabled = get(
        TasksPreferences.quietHoursEnabled,
        notificationDefaults.quietHoursEnabled
    ),
    quietHoursStart = get(TasksPreferences.quietHoursStart, notificationDefaults.quietHoursStart),
    quietHoursEnd = get(TasksPreferences.quietHoursEnd, notificationDefaults.quietHoursEnd),
)

private fun String.orNull(): String? = takeIf { it.isNotBlank() }

expect fun platformModule(): Module
