package org.tasks.receivers

import android.content.Context
import android.content.Intent
import com.todoroo.astrid.service.TaskCompleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tasks.injection.InjectingBroadcastReceiver
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CompleteTaskReceiver : InjectingBroadcastReceiver() {
    @Inject lateinit var taskCompleter: TaskCompleter

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val taskId = intent.getLongExtra(TASK_ID, 0)
        Timber.i("Completing %s", taskId)
        GlobalScope.launch {
            taskCompleter.setComplete(taskId)
        }
    }

    companion object {
        const val TASK_ID = "id"
    }
}