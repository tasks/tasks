package org.tasks.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.SubtasksHelper;

import org.tasks.injection.InjectingRemoteViewsService;
import org.tasks.injection.ServiceComponent;
import org.tasks.locale.LocaleUtils;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeBase;
import org.tasks.themes.ThemeCache;
import org.tasks.ui.WidgetCheckBoxes;

import javax.inject.Inject;

public class ScrollableWidgetUpdateService extends InjectingRemoteViewsService {

    public static final String FILTER_ID = "org.tasks.widget.FILTER_ID";

    @Inject Database database;
    @Inject TaskService taskService;
    @Inject Preferences preferences;
    @Inject SubtasksHelper subtasksHelper;
    @Inject DefaultFilterProvider defaultFilterProvider;
    @Inject WidgetCheckBoxes widgetCheckBoxes;
    @Inject ThemeCache themeCache;

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

        String filterId = (String) extras.get(FILTER_ID);
        int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
        ThemeBase themeBase = themeCache.getThemeBase(preferences.getInt(WidgetConfigActivity.PREF_THEME + widgetId, 0));
        return new ScrollableViewsFactory(subtasksHelper, preferences, LocaleUtils.createConfigurationContext(getApplicationContext()), filterId,
                themeBase.getTextColor(), widgetId, database, taskService, defaultFilterProvider, widgetCheckBoxes);
    }

    @Override
    protected void inject(ServiceComponent component) {
        component.inject(this);
    }
}
