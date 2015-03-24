package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

public class CompleteTaskReceiver extends InjectingBroadcastReceiver {

    private static final Logger log = LoggerFactory.getLogger(CompleteTaskReceiver.class);

    public static final String TASK_ID = "id";
    public static final String TOGGLE_STATE = "flip_state";

    @Inject TaskService taskService;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        long taskId = intent.getLongExtra(TASK_ID, 0);
        boolean flipState = intent.getBooleanExtra(TOGGLE_STATE, false);
        log.info("Completing {}", taskId);
        Task task = taskService.fetchById(taskId, Task.ID, Task.COMPLETION_DATE);
        taskService.setComplete(task, !flipState || !task.isCompleted());
    }
}
