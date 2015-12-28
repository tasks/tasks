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

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.Filter;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.injection.InjectingAppWidgetProvider;
import org.tasks.widget.WidgetHelper;

import javax.inject.Inject;

import timber.log.Timber;

import static com.todoroo.astrid.api.AstridApiConstants.BROADCAST_EVENT_REFRESH;
import static org.tasks.intents.TaskIntents.getEditTaskStack;

public class TasksWidget extends InjectingAppWidgetProvider {

    @Inject Broadcaster broadcaster;
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
                broadcaster.toggleCompletedState(intent.getLongExtra(TaskEditFragment.TOKEN_ID, 0));
                break;
            case EDIT_TASK:
                getEditTaskStack(
                        context,
                        (Filter) intent.getParcelableExtra(TaskListFragment.TOKEN_FILTER),
                        intent.getLongExtra(TaskEditFragment.TOKEN_ID, 0))
                        .startActivities();
                break;
            case BROADCAST_EVENT_REFRESH:
                updateWidgets(context);
                break;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        try {
            super.onUpdate(context, appWidgetManager, appWidgetIds);

            ComponentName thisWidget = new ComponentName(context, TasksWidget.class);
            int[] ids = appWidgetManager.getAppWidgetIds(thisWidget);
            for (int id : ids) {
                appWidgetManager.updateAppWidget(id, widgetHelper.createScrollableWidget(context, id));
            }
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
    }

    public static void updateWidgets(Context context) {
        if (suppressUpdateFlag > 0 && DateUtilities.now() - suppressUpdateFlag < SUPPRESS_TIME) {
            return;
        }
        suppressUpdateFlag = 0;

        updateScrollableWidgets(context, null);
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
