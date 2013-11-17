package org.tasks.widget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

import com.todoroo.andlib.service.ContextManager;

import org.tasks.R;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ScrollableTasksWidget extends AppWidgetProvider {

    private final WidgetHelper widgetHelper = new WidgetHelper();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ContextManager.setContext(context);
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        ComponentName thisWidget = new ComponentName(context, ScrollableTasksWidget.class);
        int[] ids = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int id : ids) {
            appWidgetManager.updateAppWidget(id, widgetHelper.createScrollableWidget(context, id));
        }
    }

    public static void updateWidgets(Context context) {
        updateWidgets(context, null);
    }

    public static void updateWidget(Context context, int id) {
        updateWidgets(context, new int[]{id});
    }

    private static void updateWidgets(Context context, int[] widgetIds) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (widgetIds == null) {
            widgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ScrollableTasksWidget.class));
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.list_view);
    }
}
