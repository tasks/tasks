package com.todoroo.astrid.timers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.model.Task;

public class TimerTaskCompleteListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        Task task = PluginServices.getTaskService().fetchById(taskId, Task.ID, Task.ELAPSED_SECONDS,
                Task.TIMER_START);
        if(task == null || task.getValue(Task.TIMER_START) == 0)
            return;

        TimerPlugin.updateTimer(context, task, false);
    }

}
