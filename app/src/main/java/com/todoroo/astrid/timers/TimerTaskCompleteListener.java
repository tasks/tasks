/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

public class TimerTaskCompleteListener extends InjectingBroadcastReceiver {

    @Inject TaskDao taskDao;
    @Inject TimerPlugin timerPlugin;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1) {
            return;
        }

        Task task = taskDao.fetch(taskId, Task.ID, Task.ELAPSED_SECONDS,
                Task.TIMER_START);
        if(task == null || task.getTimerStart() == 0) {
            return;
        }

        timerPlugin.stopTimer(task);
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }
}
