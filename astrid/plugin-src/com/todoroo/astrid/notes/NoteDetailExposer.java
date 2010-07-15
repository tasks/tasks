/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskDetail;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.TaskService;

/**
 * Exposes {@link TaskDetail} for tags, i.e. "Tags: frogs, animals"
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class NoteDetailExposer extends BroadcastReceiver {

    private static TaskService staticTaskService = null;

    @Autowired
    TaskService taskService;

    @Override
    public void onReceive(Context context, Intent intent) {
        // get tags associated with this task
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        if(staticTaskService == null) {
            DependencyInjectionService.getInstance().inject(this);
            staticTaskService = taskService;
        } else {
            taskService = staticTaskService;
        }

        Task task = taskService.fetchById(taskId, Task.NOTES);
        if(task == null)
            return;
        String notes = task.getValue(Task.NOTES);
        if(notes.length() == 0)
            return;

        TaskDetail taskDetail = new TaskDetail(notes);

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
