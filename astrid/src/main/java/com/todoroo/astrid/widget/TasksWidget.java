/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.widget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;

import org.tasks.R;
import org.tasks.widget.WidgetHelper;

public class TasksWidget extends AppWidgetProvider {

    public static long suppressUpdateFlag = 0; // Timestamp--don't update widgets if this flag is non-zero and now() is within 5 minutes
    private static final long SUPPRESS_TIME = DateUtilities.ONE_MINUTE * 5;

    private static final WidgetHelper widgetHelper = new WidgetHelper();

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

    public static void applyConfigSelection(Context context, int id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Intent intent = new Intent(ContextManager.getContext(), WidgetUpdateService.class);
            intent.putExtra(WidgetUpdateService.EXTRA_WIDGET_ID, id);
            context.startService(intent);
        } else {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(id, widgetHelper.createScrollableWidget(context, id));
            updateScrollableWidgets(context, new int[]{id});
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void updateScrollableWidgets(Context context, int[] widgetIds) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (widgetIds == null) {
            widgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, TasksWidget.class));
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.list_view);
    }
}
