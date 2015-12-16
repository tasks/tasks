package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

import timber.log.Timber;

public class CompleteTaskReceiver extends InjectingBroadcastReceiver {

    public static final String TASK_ID = "id";
    public static final String TOGGLE_STATE = "flip_state";

    @Inject TaskService taskService;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        long taskId = intent.getLongExtra(TASK_ID, 0);
        boolean flipState = intent.getBooleanExtra(TOGGLE_STATE, false);
        Timber.i("Completing %s", taskId);
        Task task = taskService.fetchById(taskId, Task.ID, Task.COMPLETION_DATE);
        if (task != null) {
            taskService.setComplete(task, !flipState || !task.isCompleted());
        } else {
            Timber.e("Could not find task with id %s", taskId);
        }
    }
}
