package org.tasks.gtasks

import org.jetbrains.compose.resources.getString
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.broadcast.RefreshBroadcaster
import com.todoroo.astrid.repeats.RepeatTaskHelper
import com.todoroo.astrid.service.TaskCreator
import org.tasks.data.TaskSaver
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.filters.CaldavFilter
import org.tasks.googleapis.DefaultListProvider
import org.tasks.googleapis.GoogleTaskSynchronizer
import org.tasks.googleapis.GtasksListService
import org.tasks.googleapis.InvokerFactory
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cannot_access_account
import javax.inject.Inject

class AndroidGoogleTaskSynchronizer @Inject constructor(
    private val googleAccountManager: GoogleAccountManager,
    private val invokerFactory: InvokerFactory,
    private val caldavDao: CaldavDao,
    private val taskDao: TaskDao,
    private val taskSaver: TaskSaver,
    private val firebase: Firebase,
    private val googleTaskDao: GoogleTaskDao,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val taskDeleter: TaskDeleter,
    private val alarmDao: AlarmDao,
    private val preferences: Preferences,
    private val appPreferences: AppPreferences,
    private val taskCreator: TaskCreator,
    private val repeatTaskHelper: RepeatTaskHelper,
    private val taskCompleter: TaskCompleter,
) {
    private val defaultListProvider = object : DefaultListProvider {
        override suspend fun getDefaultList(): CaldavFilter =
            defaultFilterProvider.getDefaultList()

        override suspend fun clearDefaultList() =
            preferences.setString(R.string.p_default_list, null)
    }

    private val synchronizer = GoogleTaskSynchronizer(
        caldavDao = caldavDao,
        gtasksListService = GtasksListService(caldavDao, taskDeleter, refreshBroadcaster),
        taskDao = taskDao,
        taskSaver = taskSaver,
        reporting = firebase,
        googleTaskDao = googleTaskDao,
        defaultListProvider = defaultListProvider,
        refreshBroadcaster = refreshBroadcaster,
        taskDeleter = taskDeleter,
        alarmDao = alarmDao,
        appPreferences = appPreferences,
        repeatTaskHelper = repeatTaskHelper,
        taskCompleter = taskCompleter,
        createTask = { taskCreator.createWithValues("") },
    )

    suspend fun sync(account: CaldavAccount) {
        if (googleAccountManager.getAccount(account.username) == null) {
            account.error = getString(Res.string.cannot_access_account)
            caldavDao.update(account)
            refreshBroadcaster.broadcastRefresh()
            return
        }
        val invoker = invokerFactory.getGtasksInvoker(account.username!!)
        synchronizer.sync(account, invoker)
    }
}
