package org.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Task;

import org.tasks.injection.ForApplication;
import org.tasks.receivers.CompleteTaskReceiver;

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
        Intent intent = new Intent(context, CompleteTaskReceiver.class);
        intent.putExtra(CompleteTaskReceiver.TASK_ID, taskId);
        intent.putExtra(CompleteTaskReceiver.TOGGLE_STATE, flipState);
        context.sendBroadcast(intent);
    }

    public void taskCompleted(final long id) {
        Intent intent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_COMPLETED);
        intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, id);
        context.sendOrderedBroadcast(intent, null);
    }

    public void refresh() {
        context.sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
    }

    public void refreshLists() {
        context.sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH_LISTS));
    }

    public void taskUpdated(final Task task, final ContentValues values) {
        Intent intent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_SAVED);
        intent.putExtra(AstridApiConstants.EXTRAS_TASK, task);
        intent.putExtra(AstridApiConstants.EXTRAS_VALUES, values);
        context.sendBroadcast(intent);
    }
}
