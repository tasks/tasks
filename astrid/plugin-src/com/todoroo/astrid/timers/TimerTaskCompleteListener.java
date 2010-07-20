package com.todoroo.astrid.timers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.model.Task;

public class TimerTaskCompleteListener extends BroadcastReceiver {

    /**
     * stops timer and sets elapsed time. you still need to save the task.
     * @param task
     */
    public static void stopTimer(Task task) {
        int newElapsed = (int)((DateUtilities.now() - task.getValue(Task.TIMER_START)) / 1000L);
        task.setValue(Task.TIMER_START, 0L);
        task.setValue(Task.ELAPSED_SECONDS,
                task.getValue(Task.ELAPSED_SECONDS) + newElapsed);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        Task task = PluginServices.getTaskService().fetchById(taskId, Task.ID, Task.ELAPSED_SECONDS,
                Task.TIMER_START);
        if(task == null || task.getValue(Task.TIMER_START) == 0)
            return;

        stopTimer(task);
        PluginServices.getTaskService().save(task, true);
    }

}
