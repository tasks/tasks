/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.notifications.NotificationManager;

import javax.inject.Inject;

public class TimerTaskCompleteListener extends InjectingBroadcastReceiver {

    @Inject TaskService taskService;
    @Inject NotificationManager notificationManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1) {
            return;
        }

        Task task = taskService.fetchById(taskId, Task.ID, Task.ELAPSED_SECONDS,
                Task.TIMER_START);
        if(task == null || task.getTimerStart() == 0) {
            return;
        }

        TimerPlugin.updateTimer(notificationManager, taskService, context, task, false);
    }
}
