package org.tasks.widget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViewsService;

import com.todoroo.astrid.api.Filter;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ScrollableWidgetUpdateService extends RemoteViewsService {

    public static final String IS_DARK_THEME = "org.tasks.widget.IS_DARK_THEME";
    public static final String FILTER = "org.tasks.widget.FILTER";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        if (intent == null) {
            return null;
        }

        Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }

        Bundle bundle = extras.getBundle(FILTER);
        Filter filter = (Filter) bundle.get(FILTER);
        int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
        boolean isDarkTheme = extras.getBoolean(IS_DARK_THEME);
        return new ScrollableViewsFactory(this, filter, widgetId, isDarkTheme);
    }
}
