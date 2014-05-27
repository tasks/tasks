package org.tasks.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;

import org.tasks.injection.InjectingRemoteViewsService;

import javax.inject.Inject;

public class ScrollableWidgetUpdateService extends InjectingRemoteViewsService {

    public static final String IS_DARK_THEME = "org.tasks.widget.IS_DARK_THEME";
    public static final String FILTER = "org.tasks.widget.FILTER";

    @Inject Database database;
    @Inject TaskService taskService;
    @Inject TaskListMetadataDao taskListMetadataDao;
    @Inject TagDataService tagDataService;

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
        return new ScrollableViewsFactory(this, filter, widgetId, isDarkTheme,
                database, taskService, taskListMetadataDao, tagDataService);
    }
}
