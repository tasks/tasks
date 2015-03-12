package org.tasks.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.SubtasksHelper;

import org.tasks.injection.InjectingRemoteViewsService;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class ScrollableWidgetUpdateService extends InjectingRemoteViewsService {

    public static final String FILTER = "org.tasks.widget.FILTER";

    @Inject Database database;
    @Inject TaskService taskService;
    @Inject Preferences preferences;
    @Inject SubtasksHelper subtasksHelper;

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        stopSelf();
    }

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
        return new ScrollableViewsFactory(subtasksHelper, preferences, this, filter,
                widgetId, database, taskService);
    }
}
