package org.tasks.googleapis

import org.jetbrains.compose.resources.getString
import org.tasks.analytics.Reporting
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.TaskSaver
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.AppPreferences
import org.tasks.security.KeyStoreEncryption
import org.tasks.service.TaskDeleter
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cannot_access_account

class DesktopGoogleTasksSynchronizer(
    private val caldavDao: CaldavDao,
    taskDao: TaskDao,
    taskSaver: TaskSaver,
    reporting: Reporting,
    googleTaskDao: GoogleTaskDao,
    defaultListProvider: DefaultListProvider,
    private val refreshBroadcaster: RefreshBroadcaster,
    taskDeleter: TaskDeleter,
    alarmDao: AlarmDao,
    appPreferences: AppPreferences,
    private val encryption: KeyStoreEncryption,
    createTask: suspend () -> org.tasks.data.entity.Task,
    private val proxyAuthProvider: ProxyAuthProvider,
) {
    private val synchronizer = GoogleTaskSynchronizer(
        caldavDao = caldavDao,
        gtasksListService = GtasksListService(caldavDao, taskDeleter, refreshBroadcaster),
        taskDao = taskDao,
        taskSaver = taskSaver,
        reporting = reporting,
        googleTaskDao = googleTaskDao,
        defaultListProvider = defaultListProvider,
        refreshBroadcaster = refreshBroadcaster,
        taskDeleter = taskDeleter,
        alarmDao = alarmDao,
        appPreferences = appPreferences,
        createTask = createTask,
    )

    suspend fun sync(account: CaldavAccount) {
        if (account.password.isNullOrBlank()) {
            account.error = getString(Res.string.cannot_access_account)
            caldavDao.update(account)
            refreshBroadcaster.broadcastRefresh()
            return
        }
        val credentials = GoogleTasksCredentialsAdapter(
            account = account,
            encryption = encryption,
            proxyAuthProvider = proxyAuthProvider,
            caldavDao = caldavDao,
        )
        val invoker = GtasksInvoker(credentials)
        synchronizer.sync(account, invoker)
    }
}
