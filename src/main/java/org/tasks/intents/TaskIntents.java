package org.tasks.intents;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;

import org.tasks.R;

public class TaskIntents {

    public static Intent getNewTaskIntent(Context context, Filter filter) {
        Intent intent;
        boolean twoPaneLayout = context.getResources().getBoolean(R.bool.two_pane_layout);
        if (twoPaneLayout) {
            intent = new Intent(context, TaskListActivity.class);
            intent.putExtra(TaskListActivity.OPEN_TASK, 0L);
        } else {
            intent = new Intent(context, TaskEditActivity.class);
        }

        intent.putExtra(TaskEditFragment.OVERRIDE_FINISH_ANIM, false);
        if (filter != null) {
            intent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
            if (filter.valuesForNewTasks != null) {
                String values = AndroidUtilities.contentValuesToSerializedString(filter.valuesForNewTasks);
                intent.putExtra(TaskEditFragment.TOKEN_VALUES, values);
                intent.setAction("E" + values);
            }
            if (twoPaneLayout) {
                if (filter instanceof FilterWithCustomIntent) {
                    Bundle customExtras = ((FilterWithCustomIntent) filter).customExtras;
                    intent.putExtras(customExtras);
                }
            }
        } else {
            intent.setAction("E");
        }
        return intent;
    }

    public static TaskStackBuilder getEditTaskStack(Context context, final Filter filter, final long taskId) {
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        boolean twoPaneLayout = context.getResources().getBoolean(R.bool.two_pane_layout);
        if (twoPaneLayout) {
            taskStackBuilder.addNextIntent(new Intent(context, TaskListActivity.class) {{
                putExtra(TaskListActivity.OPEN_TASK, taskId);
                if (filter != null && filter instanceof FilterWithCustomIntent) {
                    Bundle customExtras = ((FilterWithCustomIntent) filter).customExtras;
                    putExtras(customExtras);
                }
            }});
        } else {
            taskStackBuilder.addParentStack(TaskEditActivity.class);
            taskStackBuilder.addNextIntent(new Intent(context, TaskEditActivity.class) {{
                putExtra(TaskEditFragment.TOKEN_ID, taskId);
            }});
            if (filter != null) {
                taskStackBuilder.editIntentAt(0).putExtra(TaskListFragment.TOKEN_FILTER, filter);
            }
        }
        return taskStackBuilder;
    }
}
