package org.tasks.intents;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;

import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.Filter;

public class TaskIntents {

    public static TaskStackBuilder getEditTaskStack(Context context, final Filter filter, final long taskId) {
        return TaskStackBuilder.create(context).addNextIntent(getEditTaskIntent(context, filter, taskId));
    }

    public static Intent getNewTaskIntent(Context context, Filter filter) {
        return getEditTaskIntent(context, filter, 0L);
    }

    public static Intent getEditTaskIntent(Context context, final Filter filter, final long taskId) {
        Intent taskListIntent = getTaskListIntent(context, filter);
        taskListIntent.putExtra(TaskListActivity.OPEN_TASK, taskId);
        return taskListIntent;
    }

    public static Intent getTaskListIntent(Context context, final Filter filter) {
        Intent intent = new Intent(context, TaskListActivity.class);
        if (filter != null) {
            intent.putExtra(TaskListActivity.OPEN_FILTER, filter);
        }
        return intent;
    }
}
