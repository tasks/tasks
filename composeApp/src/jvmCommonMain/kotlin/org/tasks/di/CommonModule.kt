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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.tasks.TaskEditDestination
import org.tasks.analytics.Reporting
import org.tasks.audio.SoundPlayer
import org.tasks.auth.TasksOAuthClient
import org.tasks.broadcast.ComposeRefreshBroadcaster
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.CaldavClientFactory
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.CaldavSynchronizer
import org.tasks.calendars.CalendarHelper
import org.tasks.compose.accounts.AddAccountViewModel
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.data.MergedGeofence
import org.tasks.data.SubtaskTreeRegistry
import org.tasks.data.SubtaskTreeWriter
import org.tasks.data.TaskCreator
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
import org.tasks.etebase.EtebaseClientFactory
import org.tasks.etebase.EtebaseClientProvider
import org.tasks.etebase.EtebaseSynchronizer
import org.tasks.extensions.guarded
import org.tasks.filters.CaldavListCache
import org.tasks.filters.FilterPreferenceCodec
import org.tasks.filters.FilterProvider
import org.tasks.googleapis.DefaultListProvider
import org.tasks.googleapis.DesktopGoogleTasksSynchronizer
import org.tasks.googleapis.GoogleTasksCredentialsAdapter
import org.tasks.googleapis.GtasksInvoker
import org.tasks.http.KtorClientFactory
import org.tasks.http.OkHttpClientFactory
import org.tasks.http.OkHttpKtorClientFactory
import org.tasks.http.toKtor
import org.tasks.jobs.BackgroundWork
import org.tasks.jobs.RefreshScheduler
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
import org.tasks.preferences.DrawerSettings
import org.tasks.preferences.LookAndFeelSettings
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
import org.tasks.repeats.RepeatRuleToString
import org.tasks.security.Encryption
import org.tasks.security.KeyStoreEncryption
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter
import org.tasks.service.TaskMigrator
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncRunner
import org.tasks.sync.SyncSource
import org.tasks.sync.microsoft.MicrosoftSynchronizer
import org.tasks.tags.TagPickerViewModel
import org.tasks.tasklist.HeaderFormatter
import org.tasks.viewmodel.AppViewModel
import org.tasks.viewmodel.DrawerViewModel
import org.tasks.viewmodel.FilterPickerViewModel
import org.tasks.viewmodel.GoogleTaskListSettingsViewModel
import org.tasks.viewmodel.HelpAndFeedbackViewModel
import org.tasks.viewmodel.LocalAccountViewModel
import org.tasks.viewmodel.LocalListSettingsViewModel
import org.tasks.viewmodel.MainSettingsViewModel
import org.tasks.viewmodel.OpenTaskAccountViewModel
import org.tasks.viewmodel.PendingTaskSaves
import org.tasks.viewmodel.ReminderChange
import org.tasks.viewmodel.SortSettingsViewModel
import org.tasks.viewmodel.TaskEditViewModel
import org.tasks.viewmodel.TaskListViewModel
import java.util.Locale

val commonModule = module {
    includes(coreModule)

    single<BackgroundWork> {
        val scope = get<CoroutineScope>()
        val refreshScheduler = get<RefreshScheduler>()
        val runner = SyncRunner(scope, get(), { get() }) { pass ->
            val caldavSynchronizer = get<CaldavSynchronizer>()
            val etebaseSynchronizer = get<EtebaseSynchronizer>()
            pass.accounts(TYPE_CALDAV, TYPE_TASKS).forEach { account ->
                caldavSynchronizer.sync(account, hasPro = pass.hasPro)
            }
            pass.accounts(TYPE_ETEBASE).forEach { account ->
                etebaseSynchronizer.sync(account, hasPro = pass.hasPro)
            }
            pass.accounts(TYPE_GOOGLE_TASKS).forEach { account ->
                get<DesktopGoogleTasksSynchronizer>()
                    .sync(account, hasPro = pass.googleAndMicrosoftPro)
            }
            val microsoftAccounts = pass.accounts(TYPE_MICROSOFT)
            if (microsoftAccounts.isNotEmpty()) {
                val microsoftSynchronizer = get<MicrosoftSynchronizer>()
                coroutineScope {
                    microsoftAccounts.forEach { account ->
                        launch {
                            microsoftSynchronizer
                                .sync(account, hasPro = pass.googleAndMicrosoftPro)
                        }
                    }
                }
            }
            get<OpenTasksSyncer>().sync(hasPro = pass.hasPro)
        }
        object : BackgroundWork {
            override fun updateCalendar(task: Task) {}
            override suspend fun scheduleRefresh(timestamp: Long) =
                refreshScheduler.schedule(timestamp)
            override suspend fun scheduleBlogFeedCheck() {}
            override fun migrateLocalTasks(
                localAccount: CaldavAccount,
                tasksAccount: CaldavAccount,
            ) {
                scope.launch {
                    get<TaskMigrator>().migrateLocalTasks(localAccount, tasksAccount)
                }
            }
            override suspend fun sync(source: SyncSource) = runner.sync(source)
        }
    }
    factory<KtorClientFactory> { OkHttpKtorClientFactory(get()) }
    factory<CaldavClientProvider> {
        CaldavClientProvider(
            encryption = get(),
            tasksPreferences = get(),
            environment = get(),
            httpClientFactory = get(),
            tokenProvider = getOrNull(),
        )
    }
    factory<CaldavClientFactory> { get<CaldavClientProvider>() }
    factory<Encryption> { get<KeyStoreEncryption>() }
    factory<EtebaseClientFactory> { get<EtebaseClientProvider>() }
    factoryOf(::EtebaseSynchronizer)
    single { TasksOAuthClient(httpClient = runBlocking { get<OkHttpClientFactory>().newClient() }.toKtor()) }
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
            oauthClient = get(),
        )
    }
    single { Locale.getDefault() }
    viewModel { params ->
        GoogleTaskListSettingsViewModel(
            caldavDao = get(),
            taskDeleter = get(),
            reporting = get(),
            purchaseState = get(),
            invokerFactory = { account ->
                GtasksInvoker(
                    GoogleTasksCredentialsAdapter(
                        account = account,
                        encryption = get(),
                        proxyAuthProvider = get(),
                        caldavDao = get(),
                        oauthClient = get(),
                    )
                )
            },
            isDark = params.get(),
            account = params.get(),
            calendar = params.get(),
        )
    }
}
