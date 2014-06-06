/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.widget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.injection.InjectingAppWidgetProvider;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.widget.WidgetHelper;

import javax.inject.Inject;

import static com.todoroo.astrid.api.AstridApiConstants.BROADCAST_EVENT_TASK_LIST_UPDATED;

public class TasksWidget extends InjectingAppWidgetProvider {

    @Inject TaskService taskService;
    @Inject WidgetHelper widgetHelper;

    public static final String COMPLETE_TASK = "COMPLETE_TASK";
    public static final String EDIT_TASK = "EDIT_TASK";

    public static long suppressUpdateFlag = 0; // Timestamp--don't update widgets if this flag is non-zero and now() is within 5 minutes
    private static final long SUPPRESS_TIME = DateUtilities.ONE_MINUTE * 5;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        switch(intent.getAction()) {
            case COMPLETE_TASK:
                Task task = taskService.fetchById(intent.getLongExtra(TaskEditFragment.TOKEN_ID, 0), Task.ID, Task.COMPLETION_DATE);
                taskService.setComplete(task, !task.isCompleted());
                break;
            case EDIT_TASK:
                if(ActivityPreferences.isTabletSized(context)) {
                    intent.setClass(context, TaskListActivity.class);
                } else {
                    intent.setClass(context, TaskEditActivity.class);
                }
                intent.setFlags(WidgetHelper.flags);
                intent.putExtra(TaskEditFragment.OVERRIDE_FINISH_ANIM, false);
                context.startActivity(intent);

                break;
            case BROADCAST_EVENT_TASK_LIST_UPDATED:
                updateWidgets(context);
                break;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        try {
            ContextManager.setContext(context);
            super.onUpdate(context, appWidgetManager, appWidgetIds);

            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                // Start in service to prevent Application Not Responding timeout
                updateWidgets(context);
            } else {
                ComponentName thisWidget = new ComponentName(context, TasksWidget.class);
                int[] ids = appWidgetManager.getAppWidgetIds(thisWidget);
                for (int id : ids) {
                    appWidgetManager.updateAppWidget(id, widgetHelper.createScrollableWidget(context, id));
                }
            }
        } catch (Exception e) {
            Log.e("astrid-update-widget", "widget update error", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public static void updateWidgets(Context context) {
        if (suppressUpdateFlag > 0 && DateUtilities.now() - suppressUpdateFlag < SUPPRESS_TIME) {
            return;
        }
        suppressUpdateFlag = 0;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            context.startService(new Intent(context, WidgetUpdateService.class));
        } else {
            updateScrollableWidgets(context, null);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void updateScrollableWidgets(Context context, int[] widgetIds) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (widgetIds == null) {
            widgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, TasksWidget.class));
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.list_view);
    }
}
