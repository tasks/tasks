package org.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Task;

import org.tasks.injection.ForApplication;
import org.tasks.receivers.CompleteTaskReceiver;
import org.tasks.receivers.FirstLaunchReceiver;

import javax.inject.Inject;

public class Broadcaster {

    private final Context context;

    @Inject
    public Broadcaster(@ForApplication Context context) {
        this.context = context;
    }

    public void toggleCompletedState(long taskId) {
        completeTask(taskId, true);
    }

    public void completeTask(long taskId) {
        completeTask(taskId, false);
    }

    private void completeTask(final long taskId, final boolean flipState) {
        sendOrderedBroadcast(new Intent(context, CompleteTaskReceiver.class) {{
            putExtra(CompleteTaskReceiver.TASK_ID, taskId);
            putExtra(CompleteTaskReceiver.TOGGLE_STATE, flipState);
        }});
    }

    public void taskCompleted(final long id) {
        sendOrderedBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_COMPLETED) {{
            putExtra(AstridApiConstants.EXTRAS_TASK_ID, id);
        }});
    }

    public void refresh() {
        context.sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
    }

    public void taskUpdated(final Task task, final ContentValues values) {
        context.sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_SAVED) {{
            putExtra(AstridApiConstants.EXTRAS_TASK, task);
            putExtra(AstridApiConstants.EXTRAS_VALUES, values);
        }});
    }

    private void sendOrderedBroadcast(Intent intent) {
        sendOrderedBroadcast(intent, null);
    }

    private void sendOrderedBroadcast(Intent intent, String permissions) {
        context.sendOrderedBroadcast(intent, permissions);
    }

    public void firstLaunch() {
        context.sendBroadcast(new Intent(context, FirstLaunchReceiver.class));
    }
}
