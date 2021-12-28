package org.tasks.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.todoroo.astrid.service.TaskCompleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tasks.injection.ApplicationScope
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CompleteTaskReceiver : BroadcastReceiver() {
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(TASK_ID, 0)
        Timber.i("Completing %s", taskId)
        scope.launch {
            taskCompleter.setComplete(taskId)
        }
    }

    companion object {
        const val TASK_ID = "id"
    }
}