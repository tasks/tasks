/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.todoroo.andlib.service.ContextManager;

public class TasksWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        try {
            ContextManager.setContext(context);
            super.onUpdate(context, appWidgetManager, appWidgetIds);

            // Start in service to prevent Application Not Responding timeout
            updateWidgets(context);
        } catch (Exception e) {
            Log.e("astrid-update-widget", "widget update error", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Update all widgets
     */
    public static void updateWidgets(Context context) {
        context.startService(new Intent(context, WidgetUpdateService.class));
    }

    /**
     * Update widget with the given id
     */
    public static void updateWidget(Context context, int id) {
        Intent intent = new Intent(ContextManager.getContext(), WidgetUpdateService.class);
        intent.putExtra(WidgetUpdateService.EXTRA_WIDGET_ID, id);
        context.startService(intent);
    }
}
