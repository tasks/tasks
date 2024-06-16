package org.tasks.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.todoroo.astrid.service.TaskCompleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tasks.data.dao.NotificationDao
import org.tasks.injection.ApplicationScope
import org.tasks.notifications.NotificationManager
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CompleteTaskReceiver : BroadcastReceiver() {
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var notificationDao: NotificationDao
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(TASK_ID, 0)
        Timber.i("Completing %s", taskId)
        scope.launch {
            if (!notificationDao.hasNotification(taskId)) {
                Timber.e("No notification found for $taskId")
                return@launch
            }
            notificationManager.cancel(taskId)
            taskCompleter.setComplete(taskId)
        }
    }

    companion object {
        const val TASK_ID = "id"
    }
}
