package org.tasks.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViewsService;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import javax.inject.Inject;
import org.tasks.injection.InjectingApplication;
import org.tasks.locale.Locale;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeCache;
import org.tasks.ui.WidgetCheckBoxes;

public class ScrollableWidgetUpdateService extends RemoteViewsService {

  @Inject TaskDao taskDao;
  @Inject Preferences preferences;
  @Inject SubtasksHelper subtasksHelper;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject WidgetCheckBoxes widgetCheckBoxes;
  @Inject ThemeCache themeCache;
  @Inject Locale locale;

  @Override
  public void onCreate() {
    super.onCreate();

    ((InjectingApplication) getApplication()).getComponent().inject(this);
  }

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

    int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
    return new ScrollableViewsFactory(
        subtasksHelper,
        preferences,
        locale.createConfigurationContext(getApplicationContext()),
        widgetId,
        taskDao,
        defaultFilterProvider,
        widgetCheckBoxes,
        themeCache);
  }
}
