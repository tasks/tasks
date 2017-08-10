package org.tasks.widget;

import android.content.ComponentName;
import android.content.Context;

import org.tasks.R;
import org.tasks.injection.ForApplication;

import javax.inject.Inject;

public class AppWidgetManager {
    private final android.appwidget.AppWidgetManager appWidgetManager;
    private Context context;

    @Inject
    public AppWidgetManager(@ForApplication Context context) {
        this.context = context;
        appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context);
    }

    public void updateWidgets() {
        int[] widgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, TasksWidget.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.list_view);
    }
}
