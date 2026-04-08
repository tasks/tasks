package org.tasks.di

import com.todoroo.astrid.alarms.AlarmCalculator
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.repeats.RepeatTaskHelper
import com.todoroo.astrid.timers.TimerPlugin
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
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
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.filters.FilterProvider
import org.tasks.jobs.BackgroundWork
import org.tasks.location.Geocoder
import org.tasks.location.LocationService
import org.tasks.location.MapPosition
import org.tasks.notifications.Notifier
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DataStoreQueryPreferences
import org.tasks.preferences.QueryPreferences
import org.tasks.preferences.TasksPreferences
import org.tasks.reminders.Random
import org.tasks.service.TaskCleanup
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.tasklist.HeaderFormatter
import org.tasks.viewmodel.AddAccountViewModel
import org.tasks.viewmodel.AppViewModel
import org.tasks.viewmodel.DrawerViewModel
import org.tasks.viewmodel.SortSettingsViewModel
import org.tasks.viewmodel.TaskListViewModel

val commonModule = module {
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
            override suspend fun cancel(id: Long) {}
            override suspend fun cancel(ids: Iterable<Long>) {}
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
        object : org.tasks.compose.drawer.DrawerConfiguration {}
    }
    factory<org.tasks.billing.PurchaseState> {
        object : org.tasks.billing.PurchaseState {
            override val hasPro = true // Tasks.org cloud users have pro
        }
    }

    // Stateful singletons
    single<BackgroundWork> {
        val mutex = kotlinx.coroutines.sync.Mutex()
        val pending = java.util.concurrent.atomic.AtomicBoolean(false)
        object : BackgroundWork {
            override fun updateCalendar(task: Task) {}
            override suspend fun scheduleRefresh(timestamp: Long) {}
            override suspend fun sync(source: SyncSource) {
                if (!mutex.tryLock()) {
                    pending.set(true)
                    return
                }
                try {
                    do {
                        pending.set(false)
                        val synchronizer = get<CaldavSynchronizer>()
                        val caldavDao = get<org.tasks.data.dao.CaldavDao>()
                        caldavDao.getAccounts().forEach { account ->
                            synchronizer.sync(account, hasPro = account.isTasksOrg)
                        }
                    } while (pending.getAndSet(false))
                } finally {
                    mutex.unlock()
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
    factory { AlarmCalculator(Random(), 0) }
    factoryOf(::AlarmService)
    factory { RepeatTaskHelper(get(), get(), get()) }
    factory { TaskCompleter(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factoryOf(::TimerPlugin)
    factoryOf(::TaskDeleter)
    factoryOf(::TaskSaver)
    factoryOf(::iCalendar)
    factoryOf(::CaldavSynchronizer)
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
        SortSettingsViewModel(
            preferences = get(),
            reporting = get(),
            refreshBroadcaster = get(),
        )
    }
}

expect fun platformModule(): Module
